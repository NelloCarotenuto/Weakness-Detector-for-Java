/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.distributed;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.stream.Collectors;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.failure.FailureContext;
import org.apache.ignite.failure.FailureType;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.InvalidEnvironmentException;
import org.apache.ignite.internal.NodeStoppingException;
import org.apache.ignite.internal.pagemem.wal.WALPointer;
import org.apache.ignite.internal.pagemem.wal.record.DataEntry;
import org.apache.ignite.internal.pagemem.wal.record.DataRecord;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.CacheObject;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheEntryEx;
import org.apache.ignite.internal.processors.cache.GridCacheEntryRemovedException;
import org.apache.ignite.internal.processors.cache.GridCacheFilterFailedException;
import org.apache.ignite.internal.processors.cache.GridCacheMvccCandidate;
import org.apache.ignite.internal.processors.cache.GridCacheOperation;
import org.apache.ignite.internal.processors.cache.GridCacheReturn;
import org.apache.ignite.internal.processors.cache.GridCacheReturnCompletableWrapper;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.GridCacheUpdateTxResult;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearCacheEntry;
import org.apache.ignite.internal.processors.cache.persistence.StorageException;
import org.apache.ignite.internal.processors.cache.transactions.IgniteInternalTx;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxAdapter;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxEntry;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxKey;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxRemoteEx;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxRemoteState;
import org.apache.ignite.internal.processors.cache.transactions.IgniteTxState;
import org.apache.ignite.internal.processors.cache.transactions.TxCounters;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersionConflictContext;
import org.apache.ignite.internal.transactions.IgniteTxHeuristicCheckedException;
import org.apache.ignite.internal.util.future.GridFinishedFuture;
import org.apache.ignite.internal.util.lang.GridTuple;
import org.apache.ignite.internal.util.tostring.GridToStringBuilder;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionState;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.processors.cache.GridCacheOperation.CREATE;
import static org.apache.ignite.internal.processors.cache.GridCacheOperation.DELETE;
import static org.apache.ignite.internal.processors.cache.GridCacheOperation.NOOP;
import static org.apache.ignite.internal.processors.cache.GridCacheOperation.READ;
import static org.apache.ignite.internal.processors.cache.GridCacheOperation.RELOAD;
import static org.apache.ignite.internal.processors.cache.GridCacheOperation.UPDATE;
import static org.apache.ignite.internal.processors.dr.GridDrType.DR_BACKUP;
import static org.apache.ignite.internal.processors.dr.GridDrType.DR_NONE;
import static org.apache.ignite.transactions.TransactionState.COMMITTED;
import static org.apache.ignite.transactions.TransactionState.COMMITTING;
import static org.apache.ignite.transactions.TransactionState.PREPARED;
import static org.apache.ignite.transactions.TransactionState.PREPARING;
import static org.apache.ignite.transactions.TransactionState.ROLLED_BACK;
import static org.apache.ignite.transactions.TransactionState.ROLLING_BACK;
import static org.apache.ignite.transactions.TransactionState.UNKNOWN;

/**
 * Transaction created by system implicitly on remote nodes.
 */
public abstract class GridDistributedTxRemoteAdapter extends IgniteTxAdapter
    implements IgniteTxRemoteEx {
    /** */
    private static final long serialVersionUID = 0L;

    /** Commit allowed field updater. */
    private static final AtomicIntegerFieldUpdater<GridDistributedTxRemoteAdapter> COMMIT_ALLOWED_UPD =
        AtomicIntegerFieldUpdater.newUpdater(GridDistributedTxRemoteAdapter.class, "commitAllowed");

    /** Explicit versions. */
    @GridToStringInclude
    private List<GridCacheVersion> explicitVers;

    /** Started flag. */
    @GridToStringInclude
    private boolean started;

    /** {@code True} only if all write entries are locked by this transaction. */
    @SuppressWarnings("UnusedDeclaration")
    @GridToStringInclude
    private volatile int commitAllowed;

    /** */
    @GridToStringInclude
    protected IgniteTxRemoteState txState;

    /** {@code True} if tx should skip adding itself to completed version map on finish. */
    private boolean skipCompletedVers;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridDistributedTxRemoteAdapter() {
        // No-op.
    }

    /**
     * @param ctx Cache registry.
     * @param nodeId Node ID.
     * @param xidVer XID version.
     * @param commitVer Commit version.
     * @param sys System flag.
     * @param plc IO policy.
     * @param concurrency Concurrency level (should be pessimistic).
     * @param isolation Transaction isolation.
     * @param invalidate Invalidate flag.
     * @param timeout Timeout.
     * @param txSize Expected transaction size.
     * @param subjId Subject ID.
     * @param taskNameHash Task name hash code.
     */
    public GridDistributedTxRemoteAdapter(
        GridCacheSharedContext<?, ?> ctx,
        UUID nodeId,
        GridCacheVersion xidVer,
        GridCacheVersion commitVer,
        boolean sys,
        byte plc,
        TransactionConcurrency concurrency,
        TransactionIsolation isolation,
        boolean invalidate,
        long timeout,
        int txSize,
        @Nullable UUID subjId,
        int taskNameHash
    ) {
        super(
            ctx,
            nodeId,
            xidVer,
            ctx.versions().last(),
            Thread.currentThread().getId(),
            sys,
            plc,
            concurrency,
            isolation,
            timeout,
            txSize,
            subjId,
            taskNameHash);

        this.invalidate = invalidate;

        commitVersion(commitVer);

        // Must set started flag after concurrency and isolation.
        started = true;
    }

    /** {@inheritDoc} */
    @Override public IgniteTxState txState() {
        return txState;
    }

    /** {@inheritDoc} */
    @Override public UUID eventNodeId() {
        return nodeId;
    }

    /** {@inheritDoc} */
    @Override public UUID originatingNodeId() {
        return nodeId;
    }

    /** {@inheritDoc} */
    @Override public boolean activeCachesDeploymentEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public void activeCachesDeploymentEnabled(boolean depEnabled) {
        throw new UnsupportedOperationException("Remote tx doesn't support deployment.");
    }

    /** {@inheritDoc} */
    @Override public void addActiveCache(GridCacheContext cacheCtx, boolean recovery) throws IgniteCheckedException {
        txState.addActiveCache(cacheCtx, recovery, this);
    }

    /**
     * @return Checks if transaction has no entries.
     */
    @Override public boolean empty() {
        return txState.empty();
    }

    /** {@inheritDoc} */
    @Override public void invalidate(boolean invalidate) {
        this.invalidate = invalidate;
    }

    /** {@inheritDoc} */
    @Override public Map<IgniteTxKey, IgniteTxEntry> writeMap() {
        return txState.writeMap();
    }

    /** {@inheritDoc} */
    @Override public Map<IgniteTxKey, IgniteTxEntry> readMap() {
        return txState.readMap();
    }

    /** {@inheritDoc} */
    @Override public void seal() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public GridTuple<CacheObject> peek(GridCacheContext cacheCtx,
        boolean failFast,
        KeyCacheObject key)
        throws GridCacheFilterFailedException {
        assert false : "Method peek can only be called on user transaction: " + this;

        throw new IllegalStateException("Method peek can only be called on user transaction: " + this);
    }

    /** {@inheritDoc} */
    @Override public IgniteTxEntry entry(IgniteTxKey key) {
        return txState.entry(key);
    }

    /**
     * Clears entry from transaction as it never happened.
     *
     * @param key key to be removed.
     */
    public void clearEntry(IgniteTxKey key) {
        txState.clearEntry(key);
    }

    /**
     * @param baseVer Base version.
     * @param committedVers Committed versions.
     * @param rolledbackVers Rolled back versions.
     */
    @Override public void doneRemote(GridCacheVersion baseVer,
        Collection<GridCacheVersion> committedVers,
        Collection<GridCacheVersion> rolledbackVers,
        Collection<GridCacheVersion> pendingVers) {
        Map<IgniteTxKey, IgniteTxEntry> readMap = txState.readMap();

        if (readMap != null && !readMap.isEmpty()) {
            for (IgniteTxEntry txEntry : readMap.values())
                doneRemote(txEntry, baseVer, committedVers, rolledbackVers, pendingVers);
        }

        Map<IgniteTxKey, IgniteTxEntry> writeMap = txState.writeMap();

        if (writeMap != null && !writeMap.isEmpty()) {
            for (IgniteTxEntry txEntry : writeMap.values())
                doneRemote(txEntry, baseVer, committedVers, rolledbackVers, pendingVers);
        }
    }

    /** {@inheritDoc} */
    @Override public void setPartitionUpdateCounters(long[] cntrs) {
        if (writeMap() != null && !writeMap().isEmpty() && cntrs != null && cntrs.length > 0) {
            int i = 0;

            for (IgniteTxEntry txEntry : writeMap().values()) {
                txEntry.updateCounter(cntrs[i]);

                ++i;
            }
        }
    }

    /**
     * Adds completed versions to an entry.
     *
     * @param txEntry Entry.
     * @param baseVer Base version for completed versions.
     * @param committedVers Completed versions relative to base version.
     * @param rolledbackVers Rolled back versions relative to base version.
     * @param pendingVers Pending versions.
     */
    private void doneRemote(IgniteTxEntry txEntry,
        GridCacheVersion baseVer,
        Collection<GridCacheVersion> committedVers,
        Collection<GridCacheVersion> rolledbackVers,
        Collection<GridCacheVersion> pendingVers) {
        while (true) {
            GridDistributedCacheEntry entry = (GridDistributedCacheEntry)txEntry.cached();

            try {
                // Handle explicit locks.
                GridCacheVersion doneVer = txEntry.explicitVersion() != null ? txEntry.explicitVersion() : xidVer;

                entry.doneRemote(doneVer, baseVer, pendingVers, committedVers, rolledbackVers, isSystemInvalidate());

                break;
            }
            catch (GridCacheEntryRemovedException ignored) {
                assert entry.obsoleteVersion() != null;

                if (log.isDebugEnabled())
                    log.debug("Replacing obsolete entry in remote transaction [entry=" + entry + ", tx=" + this + ']');

                // Replace the entry.
                txEntry.cached(txEntry.context().cache().entryEx(txEntry.key(), topologyVersion()));
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean onOwnerChanged(GridCacheEntryEx entry, GridCacheMvccCandidate owner) {
        if (!hasWriteKey(entry.txKey()))
            return false;

        try {
            commitIfLocked();

            return true;
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to commit remote transaction: " + this, e);

            invalidate(true);
            systemInvalidate(true);

            rollbackRemoteTx();

            return false;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isStarted() {
        return started;
    }

    /** {@inheritDoc} */
    @Override public boolean hasWriteKey(IgniteTxKey key) {
        return txState.hasWriteKey(key);
    }

    /** {@inheritDoc} */
    @Override public Set<IgniteTxKey> readSet() {
        return txState.readSet();
    }

    /** {@inheritDoc} */
    @Override public Set<IgniteTxKey> writeSet() {
        return txState.writeSet();
    }

    /** {@inheritDoc} */
    @Override public Collection<IgniteTxEntry> allEntries() {
        return txState.allEntries();
    }

    /** {@inheritDoc} */
    @Override public Collection<IgniteTxEntry> writeEntries() {
        return txState.writeEntries();
    }

    /** {@inheritDoc} */
    @Override public Collection<IgniteTxEntry> readEntries() {
        return txState.readEntries();
    }

    /**
     * @throws IgniteCheckedException If failed.
     */
    public final void prepareRemoteTx() throws IgniteCheckedException {
        // If another thread is doing prepare or rollback.
        if (!state(PREPARING)) {
            // In optimistic mode prepare may be called multiple times.
            if (state() != PREPARING || !optimistic()) {
                if (log.isDebugEnabled())
                    log.debug("Invalid transaction state for prepare: " + this);

                return;
            }
        }

        try {
            cctx.tm().prepareTx(this, null);

            if (pessimistic() || isSystemInvalidate())
                state(PREPARED);
        }
        catch (IgniteCheckedException e) {
            setRollbackOnly();

            throw e;
        }
    }

    /**
     * @throws IgniteCheckedException If commit failed.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void commitIfLocked() throws IgniteCheckedException {
        if (state() == COMMITTING) {
            for (IgniteTxEntry txEntry : writeEntries()) {
                assert txEntry != null : "Missing transaction entry for tx: " + this;

                while (true) {
                    GridCacheEntryEx entry = txEntry.cached();

                    assert entry != null : "Missing cached entry for transaction entry: " + txEntry;

                    try {
                        GridCacheVersion ver = txEntry.explicitVersion() != null ? txEntry.explicitVersion() : xidVer;

                        // If locks haven't been acquired yet, keep waiting.
                        if (!entry.lockedBy(ver)) {
                            if (log.isDebugEnabled())
                                log.debug("Transaction does not own lock for entry (will wait) [entry=" + entry +
                                    ", tx=" + this + ']');

                            return;
                        }

                        break; // While.
                    }
                    catch (GridCacheEntryRemovedException ignore) {
                        if (log.isDebugEnabled())
                            log.debug("Got removed entry while committing (will retry): " + txEntry);

                        txEntry.cached(txEntry.context().cache().entryEx(txEntry.key(), topologyVersion()));
                    }
                }
            }

            // Only one thread gets to commit.
            if (COMMIT_ALLOWED_UPD.compareAndSet(this, 0, 1)) {
                IgniteCheckedException err = null;

                Map<IgniteTxKey, IgniteTxEntry> writeMap = txState.writeMap();

                GridCacheReturnCompletableWrapper wrapper = null;

                if (!F.isEmpty(writeMap) || mvccSnapshot != null) {
                    GridCacheReturn ret = null;

                    if (!near() && !local() && onePhaseCommit()) {
                        if (needReturnValue()) {
                            ret = new GridCacheReturn(null, cctx.localNodeId().equals(otherNodeId()), true, null, true);

                            UUID origNodeId = otherNodeId(); // Originating node.

                            cctx.tm().addCommittedTxReturn(this,
                                wrapper = new GridCacheReturnCompletableWrapper(
                                    !cctx.localNodeId().equals(origNodeId) ? origNodeId : null));
                        }
                        else
                            cctx.tm().addCommittedTx(this, this.nearXidVersion(), null);
                    }

                    // Register this transaction as completed prior to write-phase to
                    // ensure proper lock ordering for removed entries.
                    cctx.tm().addCommittedTx(this);

                    AffinityTopologyVersion topVer = topologyVersion();

                    WALPointer ptr = null;

                    cctx.database().checkpointReadLock();

                    try {
                        assert !txState.mvccEnabled() || mvccSnapshot != null : "Mvcc is not initialized: " + this;

                        Collection<IgniteTxEntry> entries = near() || cctx.snapshot().needTxReadLogging() ? allEntries() : writeEntries();

                        // Data entry to write to WAL and associated with it TxEntry.
                        List<T2<DataEntry, IgniteTxEntry>> dataEntries = null;

                        batchStoreCommit(writeMap().values());

                        try {
                            // Node that for near transactions we grab all entries.
                            for (IgniteTxEntry txEntry : entries) {
                                GridCacheContext cacheCtx = txEntry.context();

                                boolean replicate = cacheCtx.isDrEnabled();

                                try {
                                    while (true) {
                                        try {
                                            GridCacheEntryEx cached = txEntry.cached();

                                            if (cached == null)
                                                txEntry.cached(cached = cacheCtx.cache().entryEx(txEntry.key(), topologyVersion()));

                                            if (near() && cacheCtx.dr().receiveEnabled()) {
                                                cached.markObsolete(xidVer);

                                                break;
                                            }

                                            GridNearCacheEntry nearCached = null;

                                            if (updateNearCache(cacheCtx, txEntry.key(), topVer))
                                                nearCached = cacheCtx.dht().near().peekExx(txEntry.key());

                                            if (!F.isEmpty(txEntry.entryProcessors()))
                                                txEntry.cached().unswap(false);

                                            IgniteBiTuple<GridCacheOperation, CacheObject> res =
                                                applyTransformClosures(txEntry, false, ret);

                                            GridCacheOperation op = res.get1();
                                            CacheObject val = res.get2();

                                            GridCacheVersion explicitVer = txEntry.conflictVersion();

                                            if (explicitVer == null)
                                                explicitVer = writeVersion();

                                            if (txEntry.ttl() == CU.TTL_ZERO)
                                                op = DELETE;

                                            boolean conflictNeedResolve = cacheCtx.conflictNeedResolve();

                                            GridCacheVersionConflictContext conflictCtx = null;

                                            if (conflictNeedResolve) {
                                                IgniteBiTuple<GridCacheOperation, GridCacheVersionConflictContext>
                                                    drRes = conflictResolve(op, txEntry, val, explicitVer, cached);

                                                assert drRes != null;

                                                conflictCtx = drRes.get2();

                                                if (conflictCtx.isUseOld())
                                                    op = NOOP;
                                                else if (conflictCtx.isUseNew()) {
                                                    txEntry.ttl(conflictCtx.ttl());
                                                    txEntry.conflictExpireTime(conflictCtx.expireTime());
                                                }
                                                else if (conflictCtx.isMerge()) {
                                                    op = drRes.get1();
                                                    val = txEntry.context().toCacheObject(conflictCtx.mergeValue());
                                                    explicitVer = writeVersion();

                                                    txEntry.ttl(conflictCtx.ttl());
                                                    txEntry.conflictExpireTime(conflictCtx.expireTime());
                                                }
                                            }
                                            else
                                                // Nullify explicit version so that innerSet/innerRemove will work as usual.
                                                explicitVer = null;

                                            GridCacheVersion dhtVer = cached.isNear() ? writeVersion() : null;

                                            if (!near() && cacheCtx.group().persistenceEnabled() && cacheCtx.group().walEnabled() &&
                                                op != NOOP && op != RELOAD && (op != READ || cctx.snapshot().needTxReadLogging())) {
                                                if (dataEntries == null)
                                                    dataEntries = new ArrayList<>(entries.size());

                                                dataEntries.add(
                                                        new T2<>(
                                                                new DataEntry(
                                                                        cacheCtx.cacheId(),
                                                                        txEntry.key(),
                                                                        val,
                                                                        op,
                                                                        nearXidVersion(),
                                                                        writeVersion(),
                                                                        0,
                                                                        txEntry.key().partition(),
                                                                        txEntry.updateCounter()
                                                                ),
                                                                txEntry
                                                        )
                                                );
                                            }

                                            if (op == CREATE || op == UPDATE) {
                                                // Invalidate only for near nodes (backups cannot be invalidated).
                                                if (isSystemInvalidate() || (isInvalidate() && cacheCtx.isNear()))
                                                    cached.innerRemove(this,
                                                        eventNodeId(),
                                                        nodeId,
                                                        false,
                                                        true,
                                                        true,
                                                        txEntry.keepBinary(),
                                                        txEntry.hasOldValue(),
                                                        txEntry.oldValue(),
                                                        topVer,
                                                        null,
                                                        replicate ? DR_BACKUP : DR_NONE,
                                                        near() ? null : explicitVer,
                                                        CU.subjectId(this, cctx),
                                                        resolveTaskName(),
                                                        dhtVer,
                                                        txEntry.updateCounter(),
                                                        mvccSnapshot());
                                                else {
                                                    assert val != null : txEntry;

                                                    GridCacheUpdateTxResult updRes = cached.innerSet(this,
                                                        eventNodeId(),
                                                        nodeId,
                                                        val,
                                                        false,
                                                        false,
                                                        txEntry.ttl(),
                                                        true,
                                                        true,
                                                        txEntry.keepBinary(),
                                                        txEntry.hasOldValue(),
                                                        txEntry.oldValue(),
                                                        topVer,
                                                        null,
                                                        replicate ? DR_BACKUP : DR_NONE,
                                                        txEntry.conflictExpireTime(),
                                                        near() ? null : explicitVer,
                                                        CU.subjectId(this, cctx),
                                                        resolveTaskName(),
                                                        dhtVer,
                                                        txEntry.updateCounter(),
                                                        mvccSnapshot());

                                                    txEntry.updateCounter(updRes.updateCounter());

                                                    if (updRes.loggedPointer() != null)
                                                        ptr = updRes.loggedPointer();

                                                    // Keep near entry up to date.
                                                    if (nearCached != null) {
                                                        CacheObject val0 = cached.valueBytes();

                                                        nearCached.updateOrEvict(xidVer,
                                                            val0,
                                                            cached.expireTime(),
                                                            cached.ttl(),
                                                            nodeId,
                                                            topVer);
                                                    }
                                                }
                                            }
                                            else if (op == DELETE) {
                                                GridCacheUpdateTxResult updRes = cached.innerRemove(this,
                                                    eventNodeId(),
                                                    nodeId,
                                                    false,
                                                    true,
                                                    true,
                                                    txEntry.keepBinary(),
                                                    txEntry.hasOldValue(),
                                                    txEntry.oldValue(),
                                                    topVer,
                                                    null,
                                                    replicate ? DR_BACKUP : DR_NONE,
                                                    near() ? null : explicitVer,
                                                    CU.subjectId(this, cctx),
                                                    resolveTaskName(),
                                                    dhtVer,
                                                    txEntry.updateCounter(),
                                                    mvccSnapshot());

                                                txEntry.updateCounter(updRes.updateCounter());

                                                if (updRes.loggedPointer() != null)
                                                    ptr = updRes.loggedPointer();

                                                // Keep near entry up to date.
                                                if (nearCached != null)
                                                    nearCached.updateOrEvict(xidVer, null, 0, 0, nodeId, topVer);
                                            }
                                            else if (op == RELOAD) {
                                                CacheObject reloaded = cached.innerReload();

                                                if (nearCached != null) {
                                                    nearCached.innerReload();

                                                    nearCached.updateOrEvict(cached.version(),
                                                        reloaded,
                                                        cached.expireTime(),
                                                        cached.ttl(),
                                                        nodeId,
                                                        topVer);
                                                }
                                            }
                                            else if (op == READ) {
                                                assert near();

                                                if (log.isDebugEnabled())
                                                    log.debug("Ignoring READ entry when committing: " + txEntry);
                                            }
                                            // No-op.
                                            else {
                                                if (conflictCtx == null || !conflictCtx.isUseOld()) {
                                                    if (txEntry.ttl() != CU.TTL_NOT_CHANGED)
                                                        cached.updateTtl(null, txEntry.ttl());

                                                    if (nearCached != null) {
                                                        CacheObject val0 = cached.valueBytes();

                                                        nearCached.updateOrEvict(xidVer,
                                                            val0,
                                                            cached.expireTime(),
                                                            cached.ttl(),
                                                            nodeId,
                                                            topVer);
                                                    }
                                                }
                                            }

                                            // Assert after setting values as we want to make sure
                                            // that if we replaced removed entries.
                                            assert
                                                txEntry.op() == READ || onePhaseCommit() ||
                                                    // If candidate is not there, then lock was explicit
                                                    // and we simply allow the commit to proceed.
                                                    !cached.hasLockCandidateUnsafe(xidVer) || cached.lockedByUnsafe(xidVer) :
                                                "Transaction does not own lock for commit [entry=" + cached +
                                                    ", tx=" + this + ']';

                                            // Break out of while loop.
                                            break;
                                        }
                                        catch (GridCacheEntryRemovedException ignored) {
                                            if (log.isDebugEnabled())
                                                log.debug("Attempting to commit a removed entry (will retry): " + txEntry);

                                            // Renew cached entry.
                                            txEntry.cached(cacheCtx.cache().entryEx(txEntry.key(), topologyVersion()));
                                        }
                                    }
                                }
                                catch (Throwable ex) {
                                    boolean isNodeStopping = X.hasCause(ex, NodeStoppingException.class);
                                    boolean hasInvalidEnvironmentIssue = X.hasCause(ex, InvalidEnvironmentException.class);

                                    // In case of error, we still make the best effort to commit,
                                    // as there is no way to rollback at this point.
                                    err = new IgniteTxHeuristicCheckedException("Commit produced a runtime exception " +
                                        "(all transaction entries will be invalidated): " + CU.txString(this), ex);

                                    if (isNodeStopping) {
                                        U.warn(log, "Failed to commit transaction, node is stopping [tx=" + this +
                                            ", err=" + ex + ']');
                                    }
                                    else if (hasInvalidEnvironmentIssue) {
                                        U.warn(log, "Failed to commit transaction, node is in invalid state and will be stopped [tx=" + this +
                                            ", err=" + ex + ']');
                                    }
                                    else
                                        U.error(log, "Commit failed.", err);

                                    state(UNKNOWN);

                                    if (hasInvalidEnvironmentIssue)
                                        cctx.kernalContext().failure().process(new FailureContext(FailureType.CRITICAL_ERROR, ex));
                                    else if (!isNodeStopping) { // Skip fair uncommit in case of node stopping or invalidation.
                                        try {
                                            // Courtesy to minimize damage.
                                            uncommit();
                                        }
                                        catch (Throwable ex1) {
                                            U.error(log, "Failed to uncommit transaction: " + this, ex1);

                                            if (ex1 instanceof Error)
                                                throw ex1;
                                        }
                                    }

                                    if (ex instanceof Error)
                                        throw (Error) ex;

                                    throw err;
                                }
                            }

                            // Apply cache size deltas.
                            applyTxSizes();

                            TxCounters txCntrs = txCounters(false);

                            // Apply update counters.
                            if (txCntrs != null)
                                cctx.tm().txHandler().applyPartitionsUpdatesCounters(txCntrs.updateCounters());

                            cctx.mvccCaching().onTxFinished(this, true);

                            if (!near() && !F.isEmpty(dataEntries) && cctx.wal() != null) {
                                // Set new update counters for data entries received from persisted tx entries.
                                List<DataEntry> entriesWithCounters = dataEntries.stream()
                                    .map(tuple -> tuple.get1().partitionCounter(tuple.get2().updateCounter()))
                                    .collect(Collectors.toList());

                                    cctx.wal().log(new DataRecord(entriesWithCounters));
                            }

                            if (ptr != null && !cctx.tm().logTxRecords())
                                cctx.wal().flush(ptr, false);
                        }
                        catch (StorageException e) {
                            err = e;

                            throw new IgniteCheckedException("Failed to log transaction record " +
                                "(transaction will be rolled back): " + this, e);
                        }
                    }
                    finally {
                        cctx.database().checkpointReadUnlock();

                        if (wrapper != null)
                            wrapper.initialize(ret);
                    }
                }

                cctx.tm().commitTx(this);

                cctx.tm().mvccFinish(this, true);

                state(COMMITTED);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public final void commitRemoteTx() throws IgniteCheckedException {
        if (optimistic())
            state(PREPARED);

        if (!state(COMMITTING)) {
            TransactionState state = state();

            // If other thread is doing commit, then no-op.
            if (state == COMMITTING || state == COMMITTED)
                return;

            if (log.isDebugEnabled())
                log.debug("Failed to set COMMITTING transaction state (will rollback): " + this);

            setRollbackOnly();

            if (!isSystemInvalidate())
                throw new IgniteCheckedException("Invalid transaction state for commit [state=" + state + ", tx=" + this + ']');

            rollbackRemoteTx();
        }

        commitIfLocked();
    }

    /**
     * Forces commit for this tx.
     *
     * @throws IgniteCheckedException If commit failed.
     */
    public void forceCommit() throws IgniteCheckedException {
        commitIfLocked();
    }

    /** {@inheritDoc} */
    @Override public IgniteInternalFuture<IgniteInternalTx> commitAsync() {
        try {
            commitRemoteTx();

            return new GridFinishedFuture<IgniteInternalTx>(this);
        }
        catch (IgniteCheckedException e) {
            return new GridFinishedFuture<>(e);
        }
    }

    /** {@inheritDoc} */
    @Override public final IgniteInternalFuture<?> salvageTx() {
        try {
            systemInvalidate(true);

            prepareRemoteTx();

            if (state() == PREPARING) {
                if (log.isDebugEnabled())
                    log.debug("Ignoring transaction in PREPARING state as it is currently handled " +
                        "by another thread: " + this);

                return null;
            }

            doneRemote(xidVersion(),
                Collections.<GridCacheVersion>emptyList(),
                Collections.<GridCacheVersion>emptyList(),
                Collections.<GridCacheVersion>emptyList());

            commitRemoteTx();
        }
        catch (IgniteCheckedException e) {
            U.error(log, "Failed to invalidate transaction: " + xidVersion(), e);
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public final void rollbackRemoteTx() {
        try {
            // Note that we don't evict near entries here -
            // they will be deleted by their corresponding transactions.
            if (state(ROLLING_BACK) || state() == UNKNOWN) {
                cctx.tm().rollbackTx(this, false, skipCompletedVers);

                TxCounters counters = txCounters(false);

                if (counters != null)
                    cctx.tm().txHandler().applyPartitionsUpdatesCounters(counters.updateCounters());

                state(ROLLED_BACK);

                cctx.mvccCaching().onTxFinished(this, false);

                cctx.tm().mvccFinish(this, false);
            }
        }
        catch (IgniteCheckedException | RuntimeException | Error e) {
            state(UNKNOWN);

            if (e instanceof IgniteCheckedException)
                throw new IgniteException(e);
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw (Error) e;
        }
    }

    /** {@inheritDoc} */
    @Override public IgniteInternalFuture<IgniteInternalTx> rollbackAsync() {
        rollbackRemoteTx();

        return new GridFinishedFuture<IgniteInternalTx>(this);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridCacheVersion> alternateVersions() {
        return explicitVers == null ? Collections.<GridCacheVersion>emptyList() : explicitVers;
    }

    /** {@inheritDoc} */
    @Override public void commitError(Throwable e) {
        // No-op.
    }

    /**
     * @return {@code True} if tx should skip adding itself to completed version map on finish.
     */
    public boolean skipCompletedVersions() {
        return skipCompletedVers;
    }

    /**
     * @param skipCompletedVers {@code True} if tx should skip adding itself to completed version map on finish.
     */
    public void skipCompletedVersions(boolean skipCompletedVers) {
        this.skipCompletedVers = skipCompletedVers;
    }

    /**
     * Adds explicit version if there is one.
     *
     * @param e Transaction entry.
     */
    protected void addExplicit(IgniteTxEntry e) {
        if (e.explicitVersion() != null) {
            if (explicitVers == null)
                explicitVers = new LinkedList<>();

            if (!explicitVers.contains(e.explicitVersion())) {
                explicitVers.add(e.explicitVersion());

                if (log.isDebugEnabled())
                    log.debug("Added explicit version to transaction [explicitVer=" + e.explicitVersion() +
                        ", tx=" + this + ']');

                // Register alternate version with TM.
                cctx.tm().addAlternateVersion(e.explicitVersion(), this);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridDistributedTxRemoteAdapter.class, this, "super", super.toString());
    }
}
