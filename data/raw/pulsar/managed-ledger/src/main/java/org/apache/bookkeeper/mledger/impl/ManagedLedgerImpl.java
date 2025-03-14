/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bookkeeper.mledger.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static org.apache.bookkeeper.mledger.impl.ManagedCursorImpl.FALSE;
import static org.apache.bookkeeper.mledger.util.SafeRun.safeRun;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Range;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.bookkeeper.client.AsyncCallback.CreateCallback;
import org.apache.bookkeeper.client.AsyncCallback.OpenCallback;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.BookKeeper.DigestType;
import org.apache.bookkeeper.client.LedgerHandle;
import org.apache.bookkeeper.client.api.ReadHandle;
import org.apache.bookkeeper.common.util.Backoff;
import org.apache.bookkeeper.common.util.OrderedExecutor;
import org.apache.bookkeeper.common.util.OrderedScheduler;
import org.apache.bookkeeper.common.util.Retries;
import org.apache.bookkeeper.mledger.AsyncCallbacks.AddEntryCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.CloseCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.DeleteCursorCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.DeleteLedgerCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.OffloadCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.OpenCursorCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.ReadEntriesCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.ReadEntryCallback;
import org.apache.bookkeeper.mledger.AsyncCallbacks.TerminateCallback;
import org.apache.bookkeeper.mledger.Entry;
import org.apache.bookkeeper.mledger.ManagedCursor;
import org.apache.bookkeeper.mledger.ManagedLedger;
import org.apache.bookkeeper.mledger.ManagedLedgerConfig;
import org.apache.bookkeeper.mledger.ManagedLedgerException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.BadVersionException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.InvalidCursorPositionException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.ManagedLedgerAlreadyClosedException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.ManagedLedgerFencedException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.ManagedLedgerNotFoundException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.ManagedLedgerTerminatedException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.MetaStoreException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.MetadataNotFoundException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.NonRecoverableLedgerException;
import org.apache.bookkeeper.mledger.ManagedLedgerException.TooManyRequestsException;
import org.apache.bookkeeper.mledger.ManagedLedgerMXBean;
import org.apache.bookkeeper.mledger.Position;
import org.apache.bookkeeper.mledger.impl.ManagedCursorImpl.VoidCallback;
import org.apache.bookkeeper.mledger.impl.MetaStore.MetaStoreCallback;
import org.apache.bookkeeper.mledger.impl.MetaStore.Stat;
import org.apache.bookkeeper.mledger.offload.OffloadUtils;
import org.apache.bookkeeper.mledger.proto.MLDataFormats.ManagedLedgerInfo;
import org.apache.bookkeeper.mledger.proto.MLDataFormats.ManagedLedgerInfo.LedgerInfo;
import org.apache.bookkeeper.mledger.proto.MLDataFormats.NestedPositionInfo;
import org.apache.bookkeeper.mledger.proto.MLDataFormats.OffloadContext;
import org.apache.bookkeeper.mledger.util.CallbackMutex;
import org.apache.bookkeeper.mledger.util.Futures;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pulsar.common.api.proto.PulsarApi.CommandSubscribe.InitialPosition;
import org.apache.pulsar.common.util.collections.ConcurrentLongHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedLedgerImpl implements ManagedLedger, CreateCallback {
    private final static long MegaByte = 1024 * 1024;

    protected final static int AsyncOperationTimeoutSeconds = 30;

    protected final BookKeeper bookKeeper;
    protected final String name;
    private final Map<String, byte[]> ledgerMetadata;
    private final BookKeeper.DigestType digestType;

    protected ManagedLedgerConfig config;
    protected final MetaStore store;

    private final ConcurrentLongHashMap<CompletableFuture<ReadHandle>> ledgerCache = new ConcurrentLongHashMap<>(
            16 /* initial capacity */, 1 /* number of sections */);
    protected final NavigableMap<Long, LedgerInfo> ledgers = new ConcurrentSkipListMap<>();
    private volatile Stat ledgersStat;

    private final ManagedCursorContainer cursors = new ManagedCursorContainer();
    private final ManagedCursorContainer activeCursors = new ManagedCursorContainer();

    // Ever increasing counter of entries added
    static final AtomicLongFieldUpdater<ManagedLedgerImpl> ENTRIES_ADDED_COUNTER_UPDATER = AtomicLongFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, "entriesAddedCounter");
    @SuppressWarnings("unused")
    private volatile long entriesAddedCounter = 0;

    static final AtomicLongFieldUpdater<ManagedLedgerImpl> NUMBER_OF_ENTRIES_UPDATER = AtomicLongFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, "numberOfEntries");
    @SuppressWarnings("unused")
    private volatile long numberOfEntries = 0;
    static final AtomicLongFieldUpdater<ManagedLedgerImpl> TOTAL_SIZE_UPDATER = AtomicLongFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, "totalSize");
    @SuppressWarnings("unused")
    private volatile long totalSize = 0;

    // Cursors that are waiting to be notified when new entries are persisted
    final ConcurrentLinkedQueue<ManagedCursorImpl> waitingCursors;

    // This map is used for concurrent open cursor requests, where the 2nd request will attach a listener to the
    // uninitialized cursor future from the 1st request
    final Map<String, CompletableFuture<ManagedCursor>> uninitializedCursors;

    final EntryCache entryCache;

    private ScheduledFuture<?> timeoutTask;

    /**
     * This lock is held while the ledgers list is updated asynchronously on the metadata store. Since we use the store
     * version, we cannot have multiple concurrent updates.
     */
    private final CallbackMutex ledgersListMutex = new CallbackMutex();
    private final CallbackMutex trimmerMutex = new CallbackMutex();

    private final CallbackMutex offloadMutex = new CallbackMutex();
    private final static CompletableFuture<PositionImpl> NULL_OFFLOAD_PROMISE = CompletableFuture
            .completedFuture(PositionImpl.latest);
    private volatile LedgerHandle currentLedger;
    private long currentLedgerEntries = 0;
    private long currentLedgerSize = 0;
    private long lastLedgerCreatedTimestamp = 0;
    private long lastLedgerCreationFailureTimestamp = 0;
    private long lastLedgerCreationInitiationTimestamp = 0;

    private static final Random random = new Random(System.currentTimeMillis());
    private long maximumRolloverTimeMs;

    // Time period in which new write requests will not be accepted, after we fail in creating a new ledger.
    final static long WaitTimeAfterLedgerCreationFailureMs = 10000;

    volatile PositionImpl lastConfirmedEntry;

    protected static final int DEFAULT_LEDGER_DELETE_RETRIES = 3;
    protected static final int DEFAULT_LEDGER_DELETE_BACKOFF_TIME_SEC = 60;

    enum State {
        None, // Uninitialized
        LedgerOpened, // A ledger is ready to write into
        ClosingLedger, // Closing current ledger
        ClosedLedger, // Current ledger has been closed and there's no pending
                      // operation
        CreatingLedger, // Creating a new ledger
        Closed, // ManagedLedger has been closed
        Fenced, // A managed ledger is fenced when there is some concurrent
                // access from a different session/machine. In this state the
                // managed ledger will throw exception for all operations, since
                // the new instance will take over
        Terminated, // Managed ledger was terminated and no more entries
                    // are allowed to be added. Reads are allowed
    }

    // define boundaries for position based seeks and searches
    enum PositionBound {
        startIncluded, startExcluded
    }

    private static final AtomicReferenceFieldUpdater<ManagedLedgerImpl, State> STATE_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, State.class, "state");
    protected volatile State state = null;

    private final OrderedScheduler scheduledExecutor;
    private final OrderedExecutor executor;
    final ManagedLedgerFactoryImpl factory;
    protected final ManagedLedgerMBeanImpl mbean;
    protected final Clock clock;

    private static final AtomicLongFieldUpdater<ManagedLedgerImpl> READ_OP_COUNT_UPDATER = AtomicLongFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, "readOpCount");
    private volatile long readOpCount = 0;
    protected static final AtomicLongFieldUpdater<ManagedLedgerImpl> ADD_OP_COUNT_UPDATER = AtomicLongFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, "addOpCount");
    private volatile long addOpCount = 0;

    private final long backloggedCursorThresholdEntries;

    // last read-operation's callback to check read-timeout on it.
    private volatile ReadEntryCallbackWrapper lastReadCallback = null;
    private static final AtomicReferenceFieldUpdater<ManagedLedgerImpl, ReadEntryCallbackWrapper> LAST_READ_CALLBACK_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(ManagedLedgerImpl.class, ReadEntryCallbackWrapper.class, "lastReadCallback");

    /**
     * Queue of pending entries to be added to the managed ledger. Typically entries are queued when a new ledger is
     * created asynchronously and hence there is no ready ledger to write into.
     */
    final ConcurrentLinkedQueue<OpAddEntry> pendingAddEntries = new ConcurrentLinkedQueue<>();

    // //////////////////////////////////////////////////////////////////////

    public ManagedLedgerImpl(ManagedLedgerFactoryImpl factory, BookKeeper bookKeeper, MetaStore store,
            ManagedLedgerConfig config, OrderedScheduler scheduledExecutor, OrderedExecutor orderedExecutor,
            final String name) {
        this.factory = factory;
        this.bookKeeper = bookKeeper;
        this.config = config;
        this.store = store;
        this.name = name;
        this.ledgerMetadata = LedgerMetadataUtils.buildBaseManagedLedgerMetadata(name);
        this.digestType = BookKeeper.DigestType.fromApiDigestType(config.getDigestType());
        this.scheduledExecutor = scheduledExecutor;
        this.executor = orderedExecutor;
        TOTAL_SIZE_UPDATER.set(this, 0);
        NUMBER_OF_ENTRIES_UPDATER.set(this, 0);
        ENTRIES_ADDED_COUNTER_UPDATER.set(this, 0);
        STATE_UPDATER.set(this, State.None);
        this.ledgersStat = null;
        this.mbean = new ManagedLedgerMBeanImpl(this);
        this.entryCache = factory.getEntryCacheManager().getEntryCache(this);
        this.waitingCursors = Queues.newConcurrentLinkedQueue();
        this.uninitializedCursors = Maps.newHashMap();
        this.clock = config.getClock();
        this.backloggedCursorThresholdEntries = factory.getConfig().getThresholdBackloggedCursor();

        // Get the next rollover time. Add a random value upto 5% to avoid rollover multiple ledgers at the same time
        this.maximumRolloverTimeMs = (long) (config.getMaximumRolloverTimeMs() * (1 + random.nextDouble() * 5 / 100.0));
    }

    synchronized void initialize(final ManagedLedgerInitializeLedgerCallback callback, final Object ctx) {
        log.info("Opening managed ledger {}", name);

        // Fetch the list of existing ledgers in the managed ledger
        store.getManagedLedgerInfo(name, config.isCreateIfMissing(), new MetaStoreCallback<ManagedLedgerInfo>() {
            @Override
            public void operationComplete(ManagedLedgerInfo mlInfo, Stat stat) {
                ledgersStat = stat;
                if (mlInfo.hasTerminatedPosition()) {
                    state = State.Terminated;
                    lastConfirmedEntry = new PositionImpl(mlInfo.getTerminatedPosition());
                    log.info("[{}] Recovering managed ledger terminated at {}", name, lastConfirmedEntry);
                }

                for (LedgerInfo ls : mlInfo.getLedgerInfoList()) {
                    ledgers.put(ls.getLedgerId(), ls);
                }

                // Last ledger stat may be zeroed, we must update it
                if (ledgers.size() > 0) {
                    final long id = ledgers.lastKey();
                    OpenCallback opencb = (rc, lh, ctx1) -> {
                        executor.executeOrdered(name, safeRun(() -> {
                            mbean.endDataLedgerOpenOp();
                            if (log.isDebugEnabled()) {
                                log.debug("[{}] Opened ledger {}: ", name, id, BKException.getMessage(rc));
                            }
                            if (rc == BKException.Code.OK) {
                                LedgerInfo info = LedgerInfo.newBuilder().setLedgerId(id)
                                        .setEntries(lh.getLastAddConfirmed() + 1).setSize(lh.getLength())
                                        .setTimestamp(clock.millis()).build();
                                ledgers.put(id, info);
                                initializeBookKeeper(callback);
                            } else if (rc == BKException.Code.NoSuchLedgerExistsException) {
                                log.warn("[{}] Ledger not found: {}", name, ledgers.lastKey());
                                ledgers.remove(ledgers.lastKey());
                                initializeBookKeeper(callback);
                            } else {
                                log.error("[{}] Failed to open ledger {}: {}", name, id, BKException.getMessage(rc));
                                callback.initializeFailed(createManagedLedgerException(rc));
                                return;
                            }
                        }));
                    };

                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Opening legder {}", name, id);
                    }
                    mbean.startDataLedgerOpenOp();
                    bookKeeper.asyncOpenLedger(id, digestType, config.getPassword(), opencb, null);
                } else {
                    initializeBookKeeper(callback);
                }
            }

            @Override
            public void operationFailed(MetaStoreException e) {
                if (e instanceof MetadataNotFoundException) {
                    callback.initializeFailed(new ManagedLedgerNotFoundException(e));
                } else {
                    callback.initializeFailed(new ManagedLedgerException(e));
                }
            }
        });

        scheduleTimeoutTask();
    }

    private synchronized void initializeBookKeeper(final ManagedLedgerInitializeLedgerCallback callback) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] initializing bookkeeper; ledgers {}", name, ledgers);
        }

        // Calculate total entries and size
        Iterator<LedgerInfo> iterator = ledgers.values().iterator();
        while (iterator.hasNext()) {
            LedgerInfo li = iterator.next();
            if (li.getEntries() > 0) {
                NUMBER_OF_ENTRIES_UPDATER.addAndGet(this, li.getEntries());
                TOTAL_SIZE_UPDATER.addAndGet(this, li.getSize());
            } else {
                iterator.remove();
                bookKeeper.asyncDeleteLedger(li.getLedgerId(), (rc, ctx) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Deleted empty ledger ledgerId={} rc={}", name, li.getLedgerId(), rc);
                    }
                }, null);
            }
        }

        if (state == State.Terminated) {
            // When recovering a terminated managed ledger, we don't need to create
            // a new ledger for writing, since no more writes are allowed.
            // We just move on to the next stage
            initializeCursors(callback);
            return;
        }

        final MetaStoreCallback<Void> storeLedgersCb = new MetaStoreCallback<Void>() {
            @Override
            public void operationComplete(Void v, Stat stat) {
                ledgersStat = stat;
                initializeCursors(callback);
            }

            @Override
            public void operationFailed(MetaStoreException e) {
                callback.initializeFailed(new ManagedLedgerException(e));
            }
        };

        // Create a new ledger to start writing
        this.lastLedgerCreationInitiationTimestamp = System.nanoTime();
        mbean.startDataLedgerCreateOp();

        asyncCreateLedger(bookKeeper, config, digestType, (rc, lh, ctx) -> {

            if (checkAndCompleteLedgerOpTask(rc, lh, ctx)) {
                return;
            }

            executor.executeOrdered(name, safeRun(() -> {
                mbean.endDataLedgerCreateOp();
                if (rc != BKException.Code.OK) {
                    callback.initializeFailed(createManagedLedgerException(rc));
                    return;
                }

                log.info("[{}] Created ledger {}", name, lh.getId());
                STATE_UPDATER.set(this, State.LedgerOpened);
                lastLedgerCreatedTimestamp = clock.millis();
                currentLedger = lh;

                lastConfirmedEntry = new PositionImpl(lh.getId(), -1);
                // bypass empty ledgers, find last ledger with Message if possible.
                while (lastConfirmedEntry.getEntryId() == -1) {
                    Map.Entry<Long, LedgerInfo> formerLedger = ledgers.lowerEntry(lastConfirmedEntry.getLedgerId());
                    if (formerLedger != null) {
                        LedgerInfo ledgerInfo = formerLedger.getValue();
                        lastConfirmedEntry = PositionImpl.get(ledgerInfo.getLedgerId(), ledgerInfo.getEntries() - 1);
                    } else {
                        break;
                    }
                }

                LedgerInfo info = LedgerInfo.newBuilder().setLedgerId(lh.getId()).setTimestamp(0).build();
                ledgers.put(lh.getId(), info);

                // Save it back to ensure all nodes exist
                store.asyncUpdateLedgerIds(name, getManagedLedgerInfo(), ledgersStat, storeLedgersCb);
            }));
        }, ledgerMetadata);
    }

    private void initializeCursors(final ManagedLedgerInitializeLedgerCallback callback) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] initializing cursors", name);
        }
        store.getCursors(name, new MetaStoreCallback<List<String>>() {
            @Override
            public void operationComplete(List<String> consumers, Stat s) {
                // Load existing cursors
                final AtomicInteger cursorCount = new AtomicInteger(consumers.size());
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Found {} cursors", name, consumers.size());
                }

                if (consumers.isEmpty()) {
                    callback.initializeComplete();
                    return;
                }

                for (final String cursorName : consumers) {
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Loading cursor {}", name, cursorName);
                    }
                    final ManagedCursorImpl cursor;
                    cursor = new ManagedCursorImpl(bookKeeper, config, ManagedLedgerImpl.this, cursorName);

                    cursor.recover(new VoidCallback() {
                        @Override
                        public void operationComplete() {
                            log.info("[{}] Recovery for cursor {} completed. pos={} -- todo={}", name, cursorName,
                                    cursor.getMarkDeletedPosition(), cursorCount.get() - 1);
                            cursor.setActive();
                            cursors.add(cursor);

                            if (cursorCount.decrementAndGet() == 0) {
                                // The initialization is now completed, register the jmx mbean
                                callback.initializeComplete();
                            }
                        }

                        @Override
                        public void operationFailed(ManagedLedgerException exception) {
                            log.warn("[{}] Recovery for cursor {} failed", name, cursorName, exception);
                            cursorCount.set(-1);
                            callback.initializeFailed(exception);
                        }
                    });
                }
            }

            @Override
            public void operationFailed(MetaStoreException e) {
                log.warn("[{}] Failed to get the cursors list", name, e);
                callback.initializeFailed(new ManagedLedgerException(e));
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Position addEntry(byte[] data) throws InterruptedException, ManagedLedgerException {
        return addEntry(data, 0, data.length);
    }

    @Override
    public Position addEntry(byte[] data, int offset, int length) throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        // Result list will contain the status exception and the resulting
        // position
        class Result {
            ManagedLedgerException status = null;
            Position position = null;
        }
        final Result result = new Result();

        asyncAddEntry(data, offset, length, new AddEntryCallback() {
            @Override
            public void addComplete(Position position, Object ctx) {
                result.position = position;
                counter.countDown();
            }

            @Override
            public void addFailed(ManagedLedgerException exception, Object ctx) {
                result.status = exception;
                counter.countDown();
            }
        }, null);

        counter.await();

        if (result.status != null) {
            log.error("[{}] Error adding entry", name, result.status);
            throw result.status;
        }

        return result.position;
    }

    @Override
    public void asyncAddEntry(final byte[] data, final AddEntryCallback callback, final Object ctx) {
        asyncAddEntry(data, 0, data.length, callback, ctx);
    }

    @Override
    public void asyncAddEntry(final byte[] data, int offset, int length, final AddEntryCallback callback,
            final Object ctx) {
        ByteBuf buffer = Unpooled.wrappedBuffer(data, offset, length);
        asyncAddEntry(buffer, callback, ctx);
    }

    @Override
    public void asyncAddEntry(ByteBuf buffer, AddEntryCallback callback, Object ctx) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] asyncAddEntry size={} state={}", name, buffer.readableBytes(), state);
        }

        OpAddEntry addOperation = OpAddEntry.create(this, buffer, callback, ctx);

        // Jump to specific thread to avoid contention from writers writing from different threads
        executor.executeOrdered(name, safeRun(() -> {
            pendingAddEntries.add(addOperation);

            internalAsyncAddEntry(addOperation);
        }));
    }

    private synchronized void internalAsyncAddEntry(OpAddEntry addOperation) {
        final State state = STATE_UPDATER.get(this);
        if (state == State.Fenced) {
            addOperation.failed(new ManagedLedgerFencedException());
            return;
        } else if (state == State.Terminated) {
            addOperation.failed(new ManagedLedgerTerminatedException("Managed ledger was already terminated"));
            return;
        } else if (state == State.Closed) {
            addOperation.failed(new ManagedLedgerAlreadyClosedException("Managed ledger was already closed"));
            return;
        }

        if (state == State.ClosingLedger || state == State.CreatingLedger) {
            // We don't have a ready ledger to write into
            // We are waiting for a new ledger to be created
            if (log.isDebugEnabled()) {
                log.debug("[{}] Queue addEntry request", name);
            }
        } else if (state == State.ClosedLedger) {
            long now = clock.millis();
            if (now < lastLedgerCreationFailureTimestamp + WaitTimeAfterLedgerCreationFailureMs) {
                // Deny the write request, since we haven't waited enough time since last attempt to create a new ledger
                pendingAddEntries.remove(addOperation);
                addOperation.failed(new ManagedLedgerException("Waiting for new ledger creation to complete"));
                return;
            }

            // No ledger and no pending operations. Create a new ledger
            if (log.isDebugEnabled()) {
                log.debug("[{}] Creating a new ledger", name);
            }
            if (STATE_UPDATER.compareAndSet(this, State.ClosedLedger, State.CreatingLedger)) {
                this.lastLedgerCreationInitiationTimestamp = System.nanoTime();
                mbean.startDataLedgerCreateOp();
                asyncCreateLedger(bookKeeper, config, digestType, this, Collections.emptyMap());
            }
        } else {
            checkArgument(state == State.LedgerOpened, "ledger=%s is not opened", state);

            // Write into lastLedger
            addOperation.setLedger(currentLedger);

            ++currentLedgerEntries;
            currentLedgerSize += addOperation.data.readableBytes();

            if (log.isDebugEnabled()) {
                log.debug("[{}] Write into current ledger lh={} entries={}", name, currentLedger.getId(),
                        currentLedgerEntries);
            }

            if (currentLedgerIsFull()) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Closing current ledger lh={}", name, currentLedger.getId());
                }
                // This entry will be the last added to current ledger
                addOperation.setCloseWhenDone(true);
                STATE_UPDATER.set(this, State.ClosingLedger);
            }

            addOperation.initiate();
        }
    }

    @Override
    public ManagedCursor openCursor(String cursorName) throws InterruptedException, ManagedLedgerException {
        return openCursor(cursorName, InitialPosition.Latest);
    }


    @Override
    public ManagedCursor openCursor(String cursorName, InitialPosition initialPosition)
            throws InterruptedException, ManagedLedgerException {
        return openCursor(cursorName, initialPosition, Collections.emptyMap());
    }

    @Override
    public ManagedCursor openCursor(String cursorName, InitialPosition initialPosition, Map<String, Long> properties)
            throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        class Result {
            ManagedCursor cursor = null;
            ManagedLedgerException exception = null;
        }
        final Result result = new Result();

        asyncOpenCursor(cursorName, initialPosition, properties, new OpenCursorCallback() {
            @Override
            public void openCursorComplete(ManagedCursor cursor, Object ctx) {
                result.cursor = cursor;
                counter.countDown();
            }

            @Override
            public void openCursorFailed(ManagedLedgerException exception, Object ctx) {
                result.exception = exception;
                counter.countDown();
            }

        }, null);

        if (!counter.await(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS)) {
            throw new ManagedLedgerException("Timeout during open-cursor operation");
        }

        if (result.exception != null) {
            log.error("Error adding entry", result.exception);
            throw result.exception;
        }

        return result.cursor;
    }

    @Override
    public void asyncOpenCursor(final String cursorName, final OpenCursorCallback callback, Object ctx) {
        this.asyncOpenCursor(cursorName, InitialPosition.Latest, callback, ctx);
    }

    @Override
    public void asyncOpenCursor(final String cursorName, final InitialPosition initialPosition,
            final OpenCursorCallback callback, final Object ctx) {
        this.asyncOpenCursor(cursorName, initialPosition, Collections.emptyMap(), callback, ctx);
    }

    @Override
    public synchronized void asyncOpenCursor(final String cursorName, final InitialPosition initialPosition,
            Map<String, Long> properties, final OpenCursorCallback callback, final Object ctx) {
        try {
            checkManagedLedgerIsOpen();
            checkFenced();
        } catch (ManagedLedgerException e) {
            callback.openCursorFailed(e, ctx);
            return;
        }

        if (uninitializedCursors.containsKey(cursorName)) {
            uninitializedCursors.get(cursorName).thenAccept(cursor -> {
                callback.openCursorComplete(cursor, ctx);
            }).exceptionally(ex -> {
                callback.openCursorFailed((ManagedLedgerException) ex, ctx);
                return null;
            });
            return;
        }
        ManagedCursor cachedCursor = cursors.get(cursorName);
        if (cachedCursor != null) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Cursor was already created {}", name, cachedCursor);
            }
            callback.openCursorComplete(cachedCursor, ctx);
            return;
        }

        // Create a new one and persist it
        if (log.isDebugEnabled()) {
            log.debug("[{}] Creating new cursor: {}", name, cursorName);
        }
        final ManagedCursorImpl cursor = new ManagedCursorImpl(bookKeeper, config, this, cursorName);
        CompletableFuture<ManagedCursor> cursorFuture = new CompletableFuture<>();
        uninitializedCursors.put(cursorName, cursorFuture);
        cursor.initialize(getLastPosition(), properties, new VoidCallback() {
            @Override
            public void operationComplete() {
                log.info("[{}] Opened new cursor: {}", name, cursor);
                cursor.setActive();
                // Update the ack position (ignoring entries that were written while the cursor was being created)
                cursor.initializeCursorPosition(initialPosition == InitialPosition.Latest ? getLastPositionAndCounter()
                        : getFirstPositionAndCounter());

                synchronized (ManagedLedgerImpl.this) {
                    cursors.add(cursor);
                    uninitializedCursors.remove(cursorName).complete(cursor);
                }
                callback.openCursorComplete(cursor, ctx);
            }

            @Override
            public void operationFailed(ManagedLedgerException exception) {
                log.warn("[{}] Failed to open cursor: {}", name, cursor);

                synchronized (ManagedLedgerImpl.this) {
                    uninitializedCursors.remove(cursorName).completeExceptionally(exception);
                }
                callback.openCursorFailed(exception, ctx);
            }
        });
    }

    @Override
    public synchronized void asyncDeleteCursor(final String consumerName, final DeleteCursorCallback callback,
            final Object ctx) {
        final ManagedCursorImpl cursor = (ManagedCursorImpl) cursors.get(consumerName);
        if (cursor == null) {
            callback.deleteCursorFailed(new ManagedLedgerException("ManagedCursor not found: " + consumerName), ctx);
            return;
        } else if (!cursor.isDurable()) {
            cursors.removeCursor(consumerName);
            callback.deleteCursorComplete(ctx);
            return;
        }

        // First remove the consumer form the MetaStore. If this operation succeeds and the next one (removing the
        // ledger from BK) don't, we end up having a loose ledger leaked but the state will be consistent.
        store.asyncRemoveCursor(ManagedLedgerImpl.this.name, consumerName, new MetaStoreCallback<Void>() {
            @Override
            public void operationComplete(Void result, Stat stat) {
                cursor.asyncDeleteCursorLedger();
                cursors.removeCursor(consumerName);

                // Redo invalidation of entries in cache
                PositionImpl slowestConsumerPosition = cursors.getSlowestReaderPosition();
                if (slowestConsumerPosition != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Doing cache invalidation up to {}", slowestConsumerPosition);
                    }
                    entryCache.invalidateEntries(slowestConsumerPosition);
                } else {
                    entryCache.clear();
                }

                trimConsumedLedgersInBackground();

                log.info("[{}] [{}] Deleted cursor", name, consumerName);
                callback.deleteCursorComplete(ctx);
            }

            @Override
            public void operationFailed(MetaStoreException e) {
                callback.deleteCursorFailed(e, ctx);
            }

        });
    }

    @Override
    public void deleteCursor(String name) throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        class Result {
            ManagedLedgerException exception = null;
        }
        final Result result = new Result();

        asyncDeleteCursor(name, new DeleteCursorCallback() {
            @Override
            public void deleteCursorComplete(Object ctx) {
                counter.countDown();
            }

            @Override
            public void deleteCursorFailed(ManagedLedgerException exception, Object ctx) {
                result.exception = exception;
                counter.countDown();
            }

        }, null);

        if (!counter.await(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS)) {
            throw new ManagedLedgerException("Timeout during delete-cursors operation");
        }

        if (result.exception != null) {
            log.error("Deleting cursor", result.exception);
            throw result.exception;
        }
    }

    @Override
    public ManagedCursor newNonDurableCursor(Position startCursorPosition) throws ManagedLedgerException {
        checkManagedLedgerIsOpen();
        checkFenced();

        return new NonDurableCursorImpl(bookKeeper, config, this, null,
                (PositionImpl) startCursorPosition);
    }

    @Override
    public ManagedCursor newNonDurableCursor(Position startCursorPosition, String cursorName)
            throws ManagedLedgerException {
        Objects.requireNonNull(cursorName, "cursor name can't be null");
        checkManagedLedgerIsOpen();
        checkFenced();

        ManagedCursor cachedCursor = cursors.get(cursorName);
        if (cachedCursor != null) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Cursor was already created {}", name, cachedCursor);
            }
            return cachedCursor;
        }

        NonDurableCursorImpl cursor = new NonDurableCursorImpl(bookKeeper, config, this, cursorName,
                (PositionImpl) startCursorPosition);

        log.info("[{}] Opened new cursor: {}", name, cursor);
        synchronized (this) {
            cursors.add(cursor);
        }

        return cursor;
    }

    @Override
    public Iterable<ManagedCursor> getCursors() {
        return cursors;
    }

    @Override
    public Iterable<ManagedCursor> getActiveCursors() {
        return activeCursors;
    }

    /**
     * Tells whether the managed ledger has any active-cursor registered.
     *
     * @return true if at least a cursor exists
     */
    public boolean hasActiveCursors() {
        return !activeCursors.isEmpty();
    }

    @Override
    public long getNumberOfEntries() {
        return NUMBER_OF_ENTRIES_UPDATER.get(this);
    }

    @Override
    public long getNumberOfActiveEntries() {
        long totalEntries = getNumberOfEntries();
        PositionImpl pos = cursors.getSlowestReaderPosition();
        if (pos == null) {
            // If there are no consumers, there are no active entries
            return 0;
        } else {
            // The slowest consumer will be in the first ledger in the list. We need to subtract the entries it has
            // already consumed in order to get the active entries count.
            return totalEntries - (pos.getEntryId() + 1);
        }
    }

    @Override
    public long getTotalSize() {
        return TOTAL_SIZE_UPDATER.get(this);
    }

    @Override
    public void checkBackloggedCursors() {
        // activate caught up cursors
        cursors.forEach(cursor -> {
            if (cursor.getNumberOfEntries() < backloggedCursorThresholdEntries) {
                cursor.setActive();
            } else {
                cursor.setInactive();
            }
        });
    }

    @Override
    public long getEstimatedBacklogSize() {

        PositionImpl pos = getMarkDeletePositionOfSlowestConsumer();

        while (true) {
            if (pos == null) {
                return 0;
            }
            long size = 0;
            final long slowestConsumerLedgerId = pos.getLedgerId();

            // Subtract size of ledgers that were already fully consumed but not trimmed yet
            synchronized (this) {
                size = getTotalSize();
                size -= ledgers.values().stream().filter(li -> li.getLedgerId() < slowestConsumerLedgerId)
                        .mapToLong(li -> li.getSize()).sum();
            }

            LedgerInfo ledgerInfo = null;
            synchronized (this) {
                ledgerInfo = ledgers.get(pos.getLedgerId());
            }
            if (ledgerInfo == null) {
                // ledger was removed
                if (pos.compareTo(getMarkDeletePositionOfSlowestConsumer()) == 0) {
                    // position still has not moved
                    return size;
                }
                // retry with new slowest consumer
                pos = getMarkDeletePositionOfSlowestConsumer();
                continue;
            }

            long numEntries = pos.getEntryId();
            if (ledgerInfo.getEntries() == 0) {
                size -= consumedLedgerSize(currentLedgerSize, currentLedgerEntries, numEntries);
                return size;
            } else {
                size -= consumedLedgerSize(ledgerInfo.getSize(), ledgerInfo.getEntries(), numEntries);
                return size;
            }
        }
    }

    long estimateBacklogFromPosition(PositionImpl pos) {
        synchronized (this) {
            LedgerInfo ledgerInfo = ledgers.get(pos.getLedgerId());
            if (ledgerInfo == null) {
                return getTotalSize(); // position no longer in managed ledger, so return total size
            }
            long sizeBeforePosLedger = ledgers.values().stream().filter(li -> li.getLedgerId() < pos.getLedgerId())
                    .mapToLong(li -> li.getSize()).sum();
            long size = getTotalSize() - sizeBeforePosLedger;

            if (pos.getLedgerId() == currentLedger.getId()) {
                return size - consumedLedgerSize(currentLedgerSize, currentLedgerEntries, pos.getEntryId());
            } else {
                return size - consumedLedgerSize(ledgerInfo.getSize(), ledgerInfo.getEntries(), pos.getEntryId());
            }
        }
    }

    private long consumedLedgerSize(long ledgerSize, long ledgerEntries, long consumedEntries) {
        if (ledgerEntries <= 0) {
            return 0;
        }
        long averageSize = ledgerSize / ledgerEntries;
        return consumedEntries >= 0 ? (consumedEntries + 1) * averageSize : 0;
    }

    @Override
    public synchronized void asyncTerminate(TerminateCallback callback, Object ctx) {
        if (state == State.Fenced) {
            callback.terminateFailed(new ManagedLedgerFencedException(), ctx);
            return;
        } else if (state == State.Terminated) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Ignoring request to terminate an already terminated managed ledger", name);
            }
            callback.terminateComplete(lastConfirmedEntry, ctx);
            return;
        }

        log.info("[{}] Terminating managed ledger", name);
        state = State.Terminated;

        LedgerHandle lh = currentLedger;
        if (log.isDebugEnabled()) {
            log.debug("[{}] Closing current writing ledger {}", name, lh.getId());
        }

        mbean.startDataLedgerCloseOp();
        lh.asyncClose((rc, lh1, ctx1) -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Close complete for ledger {}: rc = {}", name, lh.getId(), rc);
            }
            mbean.endDataLedgerCloseOp();
            if (rc != BKException.Code.OK) {
                callback.terminateFailed(createManagedLedgerException(rc), ctx);
            } else {
                lastConfirmedEntry = new PositionImpl(lh.getId(), lh.getLastAddConfirmed());
                // Store the new state in metadata
                store.asyncUpdateLedgerIds(name, getManagedLedgerInfo(), ledgersStat, new MetaStoreCallback<Void>() {
                    @Override
                    public void operationComplete(Void result, Stat stat) {
                        ledgersStat = stat;
                        log.info("[{}] Terminated managed ledger at {}", name, lastConfirmedEntry);
                        callback.terminateComplete(lastConfirmedEntry, ctx);
                    }

                    @Override
                    public void operationFailed(MetaStoreException e) {
                        log.error("[{}] Failed to terminate managed ledger: {}", name, e.getMessage());
                        callback.terminateFailed(new ManagedLedgerException(e), ctx);
                    }
                });
            }
        }, null);
    }

    @Override
    public Position terminate() throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        class Result {
            Position lastPosition = null;
            ManagedLedgerException exception = null;
        }
        final Result result = new Result();

        asyncTerminate(new TerminateCallback() {
            @Override
            public void terminateComplete(Position lastPosition, Object ctx) {
                result.lastPosition = lastPosition;
                counter.countDown();
            }

            @Override
            public void terminateFailed(ManagedLedgerException exception, Object ctx) {
                result.exception = exception;
                counter.countDown();
            }

        }, null);

        if (!counter.await(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS)) {
            throw new ManagedLedgerException("Timeout during managed ledger terminate");
        }

        if (result.exception != null) {
            log.error("[{}] Error terminating managed ledger", name, result.exception);
            throw result.exception;
        }

        return result.lastPosition;
    }

    @Override
    public boolean isTerminated() {
        return state == State.Terminated;
    }

    @Override
    public void close() throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        class Result {
            ManagedLedgerException exception = null;
        }
        final Result result = new Result();

        asyncClose(new CloseCallback() {
            @Override
            public void closeComplete(Object ctx) {
                counter.countDown();
            }

            @Override
            public void closeFailed(ManagedLedgerException exception, Object ctx) {
                result.exception = exception;
                counter.countDown();
            }

        }, null);

        if (!counter.await(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS)) {
            throw new ManagedLedgerException("Timeout during managed ledger close");
        }

        if (result.exception != null) {
            log.error("[{}] Error closing managed ledger", name, result.exception);
            throw result.exception;
        }
    }

    @Override
    public synchronized void asyncClose(final CloseCallback callback, final Object ctx) {
        State state = STATE_UPDATER.get(this);
        if (state == State.Fenced) {
            factory.close(this);
            callback.closeFailed(new ManagedLedgerFencedException(), ctx);
            return;
        } else if (state == State.Closed) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Ignoring request to close a closed managed ledger", name);
            }
            callback.closeComplete(ctx);
            return;
        }

        log.info("[{}] Closing managed ledger", name);

        factory.close(this);
        STATE_UPDATER.set(this, State.Closed);

        LedgerHandle lh = currentLedger;

        if (lh == null) {
            // No ledger to close, proceed with next step
            closeAllCursors(callback, ctx);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Closing current writing ledger {}", name, lh.getId());
        }

        mbean.startDataLedgerCloseOp();
        lh.asyncClose((rc, lh1, ctx1) -> {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Close complete for ledger {}: rc = {}", name, lh.getId(), rc);
            }
            mbean.endDataLedgerCloseOp();
            if (rc != BKException.Code.OK) {
                callback.closeFailed(createManagedLedgerException(rc), ctx);
                return;
            }

            closeAllCursors(callback, ctx);
        }, null);

        if (this.timeoutTask != null) {
            this.timeoutTask.cancel(false);
        }

    }

    private void closeAllCursors(CloseCallback callback, final Object ctx) {
        // Close all cursors in parallel
        List<CompletableFuture<Void>> futures = Lists.newArrayList();
        for (ManagedCursor cursor : cursors) {
            Futures.CloseFuture closeFuture = new Futures.CloseFuture();
            cursor.asyncClose(closeFuture, null);
            futures.add(closeFuture);
        }

        Futures.waitForAll(futures).thenRun(() -> {
            callback.closeComplete(ctx);
        }).exceptionally(exception -> {
            callback.closeFailed(ManagedLedgerException.getManagedLedgerException(exception.getCause()), ctx);
            return null;
        });
    }

    // //////////////////////////////////////////////////////////////////////
    // Callbacks

    @Override
    public synchronized void createComplete(int rc, final LedgerHandle lh, Object ctx) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] createComplete rc={} ledger={}", name, rc, lh != null ? lh.getId() : -1);
        }

        if (checkAndCompleteLedgerOpTask(rc, lh, ctx)) {
            return;
        }

        mbean.endDataLedgerCreateOp();
        if (rc != BKException.Code.OK) {
            log.error("[{}] Error creating ledger rc={} {}", name, rc, BKException.getMessage(rc));
            ManagedLedgerException status = createManagedLedgerException(rc);

            // Empty the list of pending requests and make all of them fail
            clearPendingAddEntries(status);
            lastLedgerCreationFailureTimestamp = clock.millis();
            STATE_UPDATER.set(this, State.ClosedLedger);
        } else {
            log.info("[{}] Created new ledger {}", name, lh.getId());
            ledgers.put(lh.getId(), LedgerInfo.newBuilder().setLedgerId(lh.getId()).setTimestamp(0).build());
            currentLedger = lh;
            currentLedgerEntries = 0;
            currentLedgerSize = 0;

            final MetaStoreCallback<Void> cb = new MetaStoreCallback<Void>() {
                @Override
                public void operationComplete(Void v, Stat stat) {
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Updating of ledgers list after create complete. version={}", name, stat);
                    }
                    ledgersStat = stat;
                    ledgersListMutex.unlock();
                    updateLedgersIdsComplete(stat);
                    synchronized (ManagedLedgerImpl.this) {
                        mbean.addLedgerSwitchLatencySample(System.nanoTime() - lastLedgerCreationInitiationTimestamp,
                                TimeUnit.NANOSECONDS);
                    }
                }

                @Override
                public void operationFailed(MetaStoreException e) {
                    if (e instanceof BadVersionException) {
                        synchronized (ManagedLedgerImpl.this) {
                            log.error(
                                    "[{}] Failed to udpate ledger list. z-node version mismatch. Closing managed ledger",
                                    name);
                            STATE_UPDATER.set(ManagedLedgerImpl.this, State.Fenced);
                            clearPendingAddEntries(e);
                            return;
                        }
                    }

                    log.warn("[{}] Error updating meta data with the new list of ledgers: {}", name, e.getMessage());

                    // Remove the ledger, since we failed to update the list
                    ledgers.remove(lh.getId());
                    mbean.startDataLedgerDeleteOp();
                    bookKeeper.asyncDeleteLedger(lh.getId(), (rc1, ctx1) -> {
                        mbean.endDataLedgerDeleteOp();
                        if (rc1 != BKException.Code.OK) {
                            log.warn("[{}] Failed to delete ledger {}: {}", name, lh.getId(),
                                    BKException.getMessage(rc1));
                        }
                    }, null);

                    ledgersListMutex.unlock();

                    synchronized (ManagedLedgerImpl.this) {
                        lastLedgerCreationFailureTimestamp = clock.millis();
                        STATE_UPDATER.set(ManagedLedgerImpl.this, State.ClosedLedger);
                        clearPendingAddEntries(e);
                    }
                }
            };

            updateLedgersListAfterRollover(cb);
        }
    }

    private void updateLedgersListAfterRollover(MetaStoreCallback<Void> callback) {
        if (!ledgersListMutex.tryLock()) {
            // Defer update for later
            scheduledExecutor.schedule(() -> updateLedgersListAfterRollover(callback), 100, TimeUnit.MILLISECONDS);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Updating ledgers ids with new ledger. version={}", name, ledgersStat);
        }
        store.asyncUpdateLedgerIds(name, getManagedLedgerInfo(), ledgersStat, callback);
    }

    public synchronized void updateLedgersIdsComplete(Stat stat) {
        STATE_UPDATER.set(this, State.LedgerOpened);
        lastLedgerCreatedTimestamp = clock.millis();

        if (log.isDebugEnabled()) {
            log.debug("[{}] Resending {} pending messages", name, pendingAddEntries.size());
        }

        // Process all the pending addEntry requests
        for (OpAddEntry op : pendingAddEntries) {
            op.setLedger(currentLedger);
            ++currentLedgerEntries;
            currentLedgerSize += op.data.readableBytes();

            if (log.isDebugEnabled()) {
                log.debug("[{}] Sending {}", name, op);
            }

            if (currentLedgerIsFull()) {
                STATE_UPDATER.set(this, State.ClosingLedger);
                op.setCloseWhenDone(true);
                op.initiate();
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Stop writing into ledger {} queue={}", name, currentLedger.getId(),
                            pendingAddEntries.size());
                }
                break;
            } else {
                op.initiate();
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////
    // Private helpers

    synchronized void ledgerClosed(final LedgerHandle lh) {
        final State state = STATE_UPDATER.get(this);
        LedgerHandle currentLedger = this.currentLedger;
        if (currentLedger == lh && (state == State.ClosingLedger || state == State.LedgerOpened)) {
            STATE_UPDATER.set(this, State.ClosedLedger);
        } else if (state == State.Closed) {
            // The managed ledger was closed during the write operation
            clearPendingAddEntries(new ManagedLedgerAlreadyClosedException("Managed ledger was already closed"));
            return;
        } else {
            // In case we get multiple write errors for different outstanding write request, we should close the ledger
            // just once
            return;
        }

        long entriesInLedger = lh.getLastAddConfirmed() + 1;
        if (log.isDebugEnabled()) {
            log.debug("[{}] Ledger has been closed id={} entries={}", name, lh.getId(), entriesInLedger);
        }
        if (entriesInLedger > 0) {
            LedgerInfo info = LedgerInfo.newBuilder().setLedgerId(lh.getId()).setEntries(entriesInLedger)
                    .setSize(lh.getLength()).setTimestamp(clock.millis()).build();
            ledgers.put(lh.getId(), info);
        } else {
            // The last ledger was empty, so we can discard it
            ledgers.remove(lh.getId());
            mbean.startDataLedgerDeleteOp();
            bookKeeper.asyncDeleteLedger(lh.getId(), (rc, ctx) -> {
                mbean.endDataLedgerDeleteOp();
                log.info("[{}] Delete complete for empty ledger {}. rc={}", name, lh.getId(), rc);
            }, null);
        }

        trimConsumedLedgersInBackground();

        maybeOffloadInBackground(NULL_OFFLOAD_PROMISE);

        if (!pendingAddEntries.isEmpty()) {
            // Need to create a new ledger to write pending entries
            if (log.isDebugEnabled()) {
                log.debug("[{}] Creating a new ledger", name);
            }
            STATE_UPDATER.set(this, State.CreatingLedger);
            this.lastLedgerCreationInitiationTimestamp = System.nanoTime();
            mbean.startDataLedgerCreateOp();
            asyncCreateLedger(bookKeeper, config, digestType, this, Collections.emptyMap());
        }
    }

    void clearPendingAddEntries(ManagedLedgerException e) {
        while (!pendingAddEntries.isEmpty()) {
            OpAddEntry op = pendingAddEntries.poll();
            op.failed(e);
        }
    }

    void asyncReadEntries(OpReadEntry opReadEntry) {
        final State state = STATE_UPDATER.get(this);
        if (state == State.Fenced || state == State.Closed) {
            opReadEntry.readEntriesFailed(new ManagedLedgerFencedException(), opReadEntry.ctx);
            return;
        }

        long ledgerId = opReadEntry.readPosition.getLedgerId();

        LedgerHandle currentLedger = this.currentLedger;

        if (currentLedger != null && ledgerId == currentLedger.getId()) {
            // Current writing ledger is not in the cache (since we don't want
            // it to be automatically evicted), and we cannot use 2 different
            // ledger handles (read & write)for the same ledger.
            internalReadFromLedger(currentLedger, opReadEntry);
        } else {
            LedgerInfo ledgerInfo = ledgers.get(ledgerId);
            if (ledgerInfo == null || ledgerInfo.getEntries() == 0) {
                // Cursor is pointing to a empty ledger, there's no need to try opening it. Skip this ledger and
                // move to the next one
                opReadEntry.updateReadPosition(new PositionImpl(opReadEntry.readPosition.getLedgerId() + 1, 0));
                opReadEntry.checkReadCompletion();
                return;
            }

            // Get a ledger handle to read from
            getLedgerHandle(ledgerId).thenAccept(ledger -> {
                internalReadFromLedger(ledger, opReadEntry);
            }).exceptionally(ex -> {
                log.error("[{}] Error opening ledger for reading at position {} - {}", name, opReadEntry.readPosition,
                        ex.getMessage());
                opReadEntry.readEntriesFailed(ManagedLedgerException.getManagedLedgerException(ex.getCause()),
                        opReadEntry.ctx);
                return null;
            });
        }
    }

    CompletableFuture<ReadHandle> getLedgerHandle(long ledgerId) {
        CompletableFuture<ReadHandle> ledgerHandle = ledgerCache.get(ledgerId);
        if (ledgerHandle != null) {
            return ledgerHandle;
        }

        // If not present try again and create if necessary
        return ledgerCache.computeIfAbsent(ledgerId, lid -> {
            // Open the ledger for reading if it was not already opened
            if (log.isDebugEnabled()) {
                log.debug("[{}] Asynchronously opening ledger {} for read", name, ledgerId);
            }
            mbean.startDataLedgerOpenOp();

            CompletableFuture<ReadHandle> promise = new CompletableFuture<>();

            LedgerInfo info = ledgers.get(ledgerId);
            CompletableFuture<ReadHandle> openFuture = new CompletableFuture<>();
            if (info != null && info.hasOffloadContext() && info.getOffloadContext().getComplete()) {
                UUID uid = new UUID(info.getOffloadContext().getUidMsb(), info.getOffloadContext().getUidLsb());
                // TODO: improve this to load ledger offloader by driver name recorded in metadata
                openFuture = config.getLedgerOffloader().readOffloaded(ledgerId, uid,
                        OffloadUtils.getOffloadDriverMetadata(info));
            } else {
                openFuture = bookKeeper.newOpenLedgerOp().withRecovery(!isReadOnly()).withLedgerId(ledgerId)
                        .withDigestType(config.getDigestType()).withPassword(config.getPassword()).execute();
            }
            openFuture.whenCompleteAsync((res, ex) -> {
                mbean.endDataLedgerOpenOp();
                if (ex != null) {
                    ledgerCache.remove(ledgerId, promise);
                    promise.completeExceptionally(createManagedLedgerException(ex));
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("[{}] Successfully opened ledger {} for reading", name, ledgerId);
                    }
                    promise.complete(res);
                }
            }, executor.chooseThread(name));
            return promise;
        });
    }

    void invalidateLedgerHandle(ReadHandle ledgerHandle, Throwable t) {
        long ledgerId = ledgerHandle.getId();
        if (currentLedger != null && ledgerId != currentLedger.getId()) {
            // remove handle from ledger cache since we got a (read) error
            ledgerCache.remove(ledgerId);
            if (log.isDebugEnabled()) {
                log.debug("[{}] Removed ledger {} from cache (after read error)", name, ledgerId, t);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Ledger that encountered read error is current ledger", name, t);
            }
        }
    }

    void asyncReadEntry(PositionImpl position, ReadEntryCallback callback, Object ctx) {
        LedgerHandle currentLedger = this.currentLedger;
        if (log.isDebugEnabled()) {
            log.debug("[{}] Reading entry ledger {}: {}", name, position.getLedgerId(), position.getEntryId());
        }
        if (position.getLedgerId() == currentLedger.getId()) {
            LedgerHandle ledger = currentLedger;
            asyncReadEntry(ledger, position, callback, ctx);
        } else {
            getLedgerHandle(position.getLedgerId()).thenAccept(ledger -> {
                asyncReadEntry(ledger, position, callback, ctx);
            }).exceptionally(ex -> {
                log.error("[{}] Error opening ledger for reading at position {} - {}", name, position, ex.getMessage());
                callback.readEntryFailed(ManagedLedgerException.getManagedLedgerException(ex.getCause()), ctx);
                return null;
            });
        }

    }

    private void internalReadFromLedger(ReadHandle ledger, OpReadEntry opReadEntry) {

        // Perform the read
        long firstEntry = opReadEntry.readPosition.getEntryId();
        long lastEntryInLedger;
        final ManagedCursorImpl cursor = opReadEntry.cursor;

        PositionImpl lastPosition = lastConfirmedEntry;

        if (ledger.getId() == lastPosition.getLedgerId()) {
            // For the current ledger, we only give read visibility to the last entry we have received a confirmation in
            // the managed ledger layer
            lastEntryInLedger = lastPosition.getEntryId();
        } else {
            // For other ledgers, already closed the BK lastAddConfirmed is appropriate
            lastEntryInLedger = ledger.getLastAddConfirmed();
        }

        if (firstEntry > lastEntryInLedger) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] No more messages to read from ledger={} lastEntry={} readEntry={}", name,
                        ledger.getId(), lastEntryInLedger, firstEntry);
            }

            if (currentLedger == null || ledger.getId() != currentLedger.getId()) {
                // Cursor was placed past the end of one ledger, move it to the
                // beginning of the next ledger
                Long nextLedgerId = ledgers.ceilingKey(ledger.getId() + 1);
                if (nextLedgerId != null) {
                    opReadEntry.updateReadPosition(new PositionImpl(nextLedgerId, 0));
                } else {
                    opReadEntry.updateReadPosition(new PositionImpl(ledger.getId() + 1, 0));
                }
            }

            opReadEntry.checkReadCompletion();
            return;
        }

        long lastEntry = min(firstEntry + opReadEntry.getNumberOfEntriesToRead() - 1, lastEntryInLedger);

        if (log.isDebugEnabled()) {
            log.debug("[{}] Reading entries from ledger {} - first={} last={}", name, ledger.getId(), firstEntry,
                    lastEntry);
        }
        asyncReadEntry(ledger, firstEntry, lastEntry, false, opReadEntry, opReadEntry.ctx);
    }

    protected void asyncReadEntry(ReadHandle ledger, PositionImpl position, ReadEntryCallback callback, Object ctx) {
        if (config.getReadEntryTimeoutSeconds() > 0) {
            // set readOpCount to uniquely validate if ReadEntryCallbackWrapper is already recycled
            long readOpCount = READ_OP_COUNT_UPDATER.incrementAndGet(this);
            long createdTime = System.nanoTime();
            ReadEntryCallbackWrapper readCallback = ReadEntryCallbackWrapper.create(name, position.getLedgerId(),
                    position.getEntryId(), callback, readOpCount, createdTime, ctx);
            lastReadCallback = readCallback;
            entryCache.asyncReadEntry(ledger, position, readCallback, readOpCount);
        } else {
            entryCache.asyncReadEntry(ledger, position, callback, ctx);
        }
    }

    protected void asyncReadEntry(ReadHandle ledger, long firstEntry, long lastEntry, boolean isSlowestReader,
            OpReadEntry opReadEntry, Object ctx) {
        if (config.getReadEntryTimeoutSeconds() > 0) {
            // set readOpCount to uniquely validate if ReadEntryCallbackWrapper is already recycled
            long readOpCount = READ_OP_COUNT_UPDATER.incrementAndGet(this);
            long createdTime = System.nanoTime();
            ReadEntryCallbackWrapper readCallback = ReadEntryCallbackWrapper.create(name, ledger.getId(), firstEntry,
                    opReadEntry, readOpCount, createdTime, ctx);
            lastReadCallback = readCallback;
            entryCache.asyncReadEntry(ledger, firstEntry, lastEntry, isSlowestReader, readCallback, readOpCount);
        } else {
            entryCache.asyncReadEntry(ledger, firstEntry, lastEntry, isSlowestReader, opReadEntry, ctx);
        }
    }

    static final class ReadEntryCallbackWrapper implements ReadEntryCallback, ReadEntriesCallback {

        volatile ReadEntryCallback readEntryCallback;
        volatile ReadEntriesCallback readEntriesCallback;
        String name;
        long ledgerId;
        long entryId;
        volatile long readOpCount = -1;
        private static final AtomicLongFieldUpdater<ReadEntryCallbackWrapper> READ_OP_COUNT_UPDATER = AtomicLongFieldUpdater
                .newUpdater(ReadEntryCallbackWrapper.class, "readOpCount");
        volatile long createdTime = -1;
        volatile Object cntx;

        final Handle<ReadEntryCallbackWrapper> recyclerHandle;

        private ReadEntryCallbackWrapper(Handle<ReadEntryCallbackWrapper> recyclerHandle) {
            this.recyclerHandle = recyclerHandle;
        }

        static ReadEntryCallbackWrapper create(String name, long ledgerId, long entryId, ReadEntryCallback callback,
                long readOpCount, long createdTime, Object ctx) {
            ReadEntryCallbackWrapper readCallback = RECYCLER.get();
            readCallback.name = name;
            readCallback.ledgerId = ledgerId;
            readCallback.entryId = entryId;
            readCallback.readEntryCallback = callback;
            readCallback.cntx = ctx;
            readCallback.readOpCount = readOpCount;
            readCallback.createdTime = createdTime;
            return readCallback;
        }

        static ReadEntryCallbackWrapper create(String name, long ledgerId, long entryId, ReadEntriesCallback callback,
                long readOpCount, long createdTime, Object ctx) {
            ReadEntryCallbackWrapper readCallback = RECYCLER.get();
            readCallback.name = name;
            readCallback.ledgerId = ledgerId;
            readCallback.entryId = entryId;
            readCallback.readEntriesCallback = callback;
            readCallback.cntx = ctx;
            readCallback.readOpCount = readOpCount;
            readCallback.createdTime = createdTime;
            return readCallback;
        }

        @Override
        public void readEntryComplete(Entry entry, Object ctx) {
            long reOpCount = reOpCount(ctx);
            ReadEntryCallback callback = this.readEntryCallback;
            Object cbCtx = this.cntx;
            if (recycle(reOpCount)) {
                callback.readEntryComplete(entry, cbCtx);
                return;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] read entry already completed for {}-{}", name, ledgerId, entryId);
                }
                entry.release();
                return;
            }
        }

        @Override
        public void readEntryFailed(ManagedLedgerException exception, Object ctx) {
            long reOpCount = reOpCount(ctx);
            ReadEntryCallback callback = this.readEntryCallback;
            Object cbCtx = this.cntx;
            if (recycle(reOpCount)) {
                callback.readEntryFailed(exception, cbCtx);
                return;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] read entry already completed for {}-{}", name, ledgerId, entryId);
                }
            }
        }

        @Override
        public void readEntriesComplete(List<Entry> returnedEntries, Object ctx) {
            long reOpCount = reOpCount(ctx);
            ReadEntriesCallback callback = this.readEntriesCallback;
            Object cbCtx = this.cntx;
            if (recycle(reOpCount)) {
                callback.readEntriesComplete(returnedEntries, cbCtx);
                return;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] read entry already completed for {}-{}", name, ledgerId, entryId);
                }
                returnedEntries.forEach(Entry::release);
                return;
            }
        }

        @Override
        public void readEntriesFailed(ManagedLedgerException exception, Object ctx) {
            long reOpCount = reOpCount(ctx);
            ReadEntriesCallback callback = this.readEntriesCallback;
            Object cbCtx = this.cntx;
            if (recycle(reOpCount)) {
                callback.readEntriesFailed(exception, cbCtx);
                return;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] read entry already completed for {}-{}", name, ledgerId, entryId);
                }
                return;
            }
        }

        private long reOpCount(Object ctx) {
            return (ctx != null && ctx instanceof Long) ? (long) ctx : -1;
        }

        public void readFailed(ManagedLedgerException exception, Object ctx) {
            if (readEntryCallback != null) {
                readEntryFailed(exception, ctx);
            } else if (readEntriesCallback != null) {
                readEntriesFailed(exception, ctx);
            }
            // It happens when timeout-thread and read-callback both recycles at the same time.
            // this read-callback has already been recycled so, do nothing..
        }

        private boolean recycle(long readOpCount) {
            if (readOpCount != -1
                    && READ_OP_COUNT_UPDATER.compareAndSet(ReadEntryCallbackWrapper.this, readOpCount, -1)) {
                createdTime = -1;
                readEntryCallback = null;
                readEntriesCallback = null;
                ledgerId = -1;
                entryId = -1;
                name = null;
                recyclerHandle.recycle(this);
                return true;
            }
            return false;
        }

        private static final Recycler<ReadEntryCallbackWrapper> RECYCLER = new Recycler<ReadEntryCallbackWrapper>() {
            @Override
            protected ReadEntryCallbackWrapper newObject(Handle<ReadEntryCallbackWrapper> handle) {
                return new ReadEntryCallbackWrapper(handle);
            }
        };

    }

    @Override
    public ManagedLedgerMXBean getStats() {
        return mbean;
    }

    boolean hasMoreEntries(PositionImpl position) {
        PositionImpl lastPos = lastConfirmedEntry;
        boolean result = position.compareTo(lastPos) <= 0;
        if (log.isDebugEnabled()) {
            log.debug("[{}] hasMoreEntries: pos={} lastPos={} res={}", name, position, lastPos, result);
        }
        return result;
    }

    void discardEntriesFromCache(ManagedCursorImpl cursor, PositionImpl newPosition) {
        Pair<PositionImpl, PositionImpl> pair = activeCursors.cursorUpdated(cursor, newPosition);
        if (pair != null) {
            entryCache.invalidateEntries(pair.getRight());
        }
    }

    void doCacheEviction(long maxTimestamp) {
        // Always remove all entries already read by active cursors
        PositionImpl slowestReaderPos = getEarlierReadPositionForActiveCursors();
        if (slowestReaderPos != null) {
            entryCache.invalidateEntries(slowestReaderPos);
        }

        // Remove entries older than the cutoff threshold
        entryCache.invalidateEntriesBeforeTimestamp(maxTimestamp);
    }

    private PositionImpl getEarlierReadPositionForActiveCursors() {
        PositionImpl smallest = null;
        for (ManagedCursor cursor : activeCursors) {
            PositionImpl p = (PositionImpl) cursor.getReadPosition();
            if (smallest == null) {
                smallest = p;
            } else if (p.compareTo(smallest) < 0) {
                smallest = p;
            }
        }

        return smallest;
    }

    void updateCursor(ManagedCursorImpl cursor, PositionImpl newPosition) {
        Pair<PositionImpl, PositionImpl> pair = cursors.cursorUpdated(cursor, newPosition);
        if (pair == null) {
            // Cursor has been removed in the meantime
            trimConsumedLedgersInBackground();
            return;
        }

        PositionImpl previousSlowestReader = pair.getLeft();
        PositionImpl currentSlowestReader = pair.getRight();

        if (previousSlowestReader.compareTo(currentSlowestReader) == 0) {
            // The slowest consumer has not changed position. Nothing to do right now
            return;
        }

        // Only trigger a trimming when switching to the next ledger
        if (previousSlowestReader.getLedgerId() != newPosition.getLedgerId()) {
            trimConsumedLedgersInBackground();
        }
    }

    PositionImpl startReadOperationOnLedger(PositionImpl position) {
        long ledgerId = ledgers.ceilingKey(position.getLedgerId());
        if (ledgerId != position.getLedgerId()) {
            // The ledger pointed by this position does not exist anymore. It was deleted because it was empty. We need
            // to skip on the next available ledger
            position = new PositionImpl(ledgerId, 0);
        }

        return position;
    }

    void notifyCursors() {
        while (true) {
            final ManagedCursorImpl waitingCursor = waitingCursors.poll();
            if (waitingCursor == null) {
                break;
            }

            executor.execute(safeRun(() -> waitingCursor.notifyEntriesAvailable()));
        }
    }

    private void trimConsumedLedgersInBackground() {
        trimConsumedLedgersInBackground(Futures.NULL_PROMISE);
    }

    private void trimConsumedLedgersInBackground(CompletableFuture<?> promise) {
        executor.executeOrdered(name, safeRun(() -> {
            internalTrimConsumedLedgers(promise);
        }));
    }

    private void scheduleDeferredTrimming(CompletableFuture<?> promise) {
        scheduledExecutor.schedule(safeRun(() -> trimConsumedLedgersInBackground(promise)), 100, TimeUnit.MILLISECONDS);
    }

    private void maybeOffloadInBackground(CompletableFuture<PositionImpl> promise) {
        if (config.getOffloadAutoTriggerSizeThresholdBytes() >= 0) {
            executor.executeOrdered(name, safeRun(() -> maybeOffload(promise)));
        }
    }

    private void maybeOffload(CompletableFuture<PositionImpl> finalPromise) {
        if (!offloadMutex.tryLock()) {
            scheduledExecutor.schedule(safeRun(() -> maybeOffloadInBackground(finalPromise)),
                                       100, TimeUnit.MILLISECONDS);
        } else {
            CompletableFuture<PositionImpl> unlockingPromise = new CompletableFuture<>();
            unlockingPromise.whenComplete((res, ex) -> {
                    offloadMutex.unlock();
                    if (ex != null) {
                        finalPromise.completeExceptionally(ex);
                    } else {
                        finalPromise.complete(res);
                    }
                });

            long threshold = config.getOffloadAutoTriggerSizeThresholdBytes();
            long sizeSummed = 0;
            long alreadyOffloadedSize = 0;
            long toOffloadSize = 0;

            ConcurrentLinkedDeque<LedgerInfo> toOffload = new ConcurrentLinkedDeque();

            // go through ledger list from newest to oldest and build a list to offload in oldest to newest order
            for (Map.Entry<Long, LedgerInfo> e : ledgers.descendingMap().entrySet()) {
                long size = e.getValue().getSize();
                sizeSummed += size;
                boolean alreadyOffloaded = e.getValue().hasOffloadContext()
                    && e.getValue().getOffloadContext().getComplete();
                if (alreadyOffloaded) {
                    alreadyOffloadedSize += size;
                } else if (sizeSummed > threshold) {
                    toOffloadSize += size;
                    toOffload.addFirst(e.getValue());
                }
            }

            if (toOffload.size() > 0) {
                log.info("[{}] Going to automatically offload ledgers {}"
                         + ", total size = {}, already offloaded = {}, to offload = {}",
                         name, toOffload.stream().map(l -> l.getLedgerId()).collect(Collectors.toList()),
                         sizeSummed, alreadyOffloadedSize, toOffloadSize);
            } else {
                // offloadLoop will complete immediately with an empty list to offload
                log.debug("[{}] Nothing to offload, total size = {}, already offloaded = {}, threshold = {}",
                          name, sizeSummed, alreadyOffloadedSize, threshold);
            }

            offloadLoop(unlockingPromise, toOffload, PositionImpl.latest, Optional.empty());
        }
    }

    private boolean hasLedgerRetentionExpired(long ledgerTimestamp) {
        if (config.getRetentionTimeMillis() < 0) {
            // Negative retention time equates to infinite retention
            return false;
        }

        long elapsedMs = clock.millis() - ledgerTimestamp;
        return elapsedMs > config.getRetentionTimeMillis();
    }

    private boolean isLedgerRetentionOverSizeQuota() {
        // Handle the -1 size limit as "infinite" size quota
        return config.getRetentionSizeInMB() >= 0
                && TOTAL_SIZE_UPDATER.get(this) > ((long) config.getRetentionSizeInMB()) * 1024 * 1024;
    }

    private boolean isOffloadedNeedsDelete(OffloadContext offload) {
        long elapsedMs = clock.millis() - offload.getTimestamp();
        return offload.getComplete() && !offload.getBookkeeperDeleted()
                && elapsedMs > config.getOffloadLedgerDeletionLagMillis();
    }

    /**
     * Checks whether there are ledger that have been fully consumed and deletes them.
     *
     * @throws Exception
     */
    void internalTrimConsumedLedgers(CompletableFuture<?> promise) {
        // Ensure only one trimming operation is active
        if (!trimmerMutex.tryLock()) {
            scheduleDeferredTrimming(promise);
            return;
        }

        List<LedgerInfo> ledgersToDelete = Lists.newArrayList();
        List<LedgerInfo> offloadedLedgersToDelete = Lists.newArrayList();
        synchronized (this) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Start TrimConsumedLedgers. ledgers={} totalSize={}", name, ledgers.keySet(),
                        TOTAL_SIZE_UPDATER.get(this));
            }
            if (STATE_UPDATER.get(this) == State.Closed) {
                log.debug("[{}] Ignoring trimming request since the managed ledger was already closed", name);
                trimmerMutex.unlock();
                promise.completeExceptionally(new ManagedLedgerAlreadyClosedException("Can't trim closed ledger"));
                return;
            }

            long slowestReaderLedgerId = -1;
            if (cursors.isEmpty()) {
                // At this point the lastLedger will be pointing to the
                // ledger that has just been closed, therefore the +1 to
                // include lastLedger in the trimming.
                slowestReaderLedgerId = currentLedger.getId() + 1;
            } else {
                PositionImpl slowestReaderPosition = cursors.getSlowestReaderPosition();
                if (slowestReaderPosition != null) {
                    slowestReaderLedgerId = slowestReaderPosition.getLedgerId();
                } else {
                    promise.completeExceptionally(new ManagedLedgerException("Couldn't find reader position"));
                    trimmerMutex.unlock();
                    return;
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("[{}] Slowest consumer ledger id: {}", name, slowestReaderLedgerId);
            }

            // skip ledger if retention constraint met
            for (LedgerInfo ls : ledgers.headMap(slowestReaderLedgerId, false).values()) {
                boolean expired = hasLedgerRetentionExpired(ls.getTimestamp());
                boolean overRetentionQuota = isLedgerRetentionOverSizeQuota();

                if (log.isDebugEnabled()) {
                    log.debug(
                            "[{}] Checking ledger {} -- time-old: {} sec -- "
                                    + "expired: {} -- over-quota: {} -- current-ledger: {}",
                            name, ls.getLedgerId(), (clock.millis() - ls.getTimestamp()) / 1000.0, expired,
                            overRetentionQuota, currentLedger.getId());
                }
                if (ls.getLedgerId() == currentLedger.getId()) {
                    log.debug("[{}] ledger id skipped for deletion as it is currently being written to", name,
                            ls.getLedgerId());
                    break;
                } else if (expired) {
                    log.debug("[{}] Ledger {} has expired, ts {}", name, ls.getLedgerId(), ls.getTimestamp());
                    ledgersToDelete.add(ls);
                } else if (overRetentionQuota) {
                    log.debug("[{}] Ledger {} is over quota", name, ls.getLedgerId());
                    ledgersToDelete.add(ls);
                } else {
                    log.debug("[{}] Ledger {} not deleted. Neither expired nor over-quota", name, ls.getLedgerId());
                    break;
                }
            }
            for (LedgerInfo ls : ledgers.values()) {
                if (isOffloadedNeedsDelete(ls.getOffloadContext()) && !ledgersToDelete.contains(ls)) {
                    log.debug("[{}] Ledger {} has been offloaded, bookkeeper ledger needs to be deleted", name,
                            ls.getLedgerId());
                    offloadedLedgersToDelete.add(ls);
                }
            }

            if (ledgersToDelete.isEmpty() && offloadedLedgersToDelete.isEmpty()) {
                trimmerMutex.unlock();
                promise.complete(null);
                return;
            }

            if (STATE_UPDATER.get(this) == State.CreatingLedger // Give up now and schedule a new trimming
                    || !ledgersListMutex.tryLock()) { // Avoid deadlocks with other operations updating the ledgers list
                scheduleDeferredTrimming(promise);
                trimmerMutex.unlock();
                return;
            }

            // Update metadata
            for (LedgerInfo ls : ledgersToDelete) {
                ledgerCache.remove(ls.getLedgerId());

                ledgers.remove(ls.getLedgerId());
                NUMBER_OF_ENTRIES_UPDATER.addAndGet(this, -ls.getEntries());
                TOTAL_SIZE_UPDATER.addAndGet(this, -ls.getSize());

                entryCache.invalidateAllEntries(ls.getLedgerId());
            }
            for (LedgerInfo ls : offloadedLedgersToDelete) {
                LedgerInfo.Builder newInfoBuilder = ls.toBuilder();
                newInfoBuilder.getOffloadContextBuilder().setBookkeeperDeleted(true);
                String driverName = OffloadUtils.getOffloadDriverName(ls,
                        config.getLedgerOffloader().getOffloadDriverName());
                Map<String, String> driverMetadata = OffloadUtils.getOffloadDriverMetadata(ls,
                        config.getLedgerOffloader().getOffloadDriverMetadata());
                OffloadUtils.setOffloadDriverMetadata(newInfoBuilder, driverName, driverMetadata);
                ledgers.put(ls.getLedgerId(), newInfoBuilder.build());
            }

            if (log.isDebugEnabled()) {
                log.debug("[{}] Updating of ledgers list after trimming", name);
            }

            store.asyncUpdateLedgerIds(name, getManagedLedgerInfo(), ledgersStat, new MetaStoreCallback<Void>() {
                @Override
                public void operationComplete(Void result, Stat stat) {
                    log.info("[{}] End TrimConsumedLedgers. ledgers={} totalSize={}", name, ledgers.size(),
                            TOTAL_SIZE_UPDATER.get(ManagedLedgerImpl.this));
                    ledgersStat = stat;
                    ledgersListMutex.unlock();
                    trimmerMutex.unlock();

                    for (LedgerInfo ls : ledgersToDelete) {
                        log.info("[{}] Removing ledger {} - size: {}", name, ls.getLedgerId(), ls.getSize());
                        asyncDeleteLedger(ls.getLedgerId(), ls);
                    }
                    for (LedgerInfo ls : offloadedLedgersToDelete) {
                        log.info("[{}] Deleting offloaded ledger {} from bookkeeper - size: {}", name, ls.getLedgerId(),
                                ls.getSize());
                        asyncDeleteLedgerFromBookKeeper(ls.getLedgerId());
                    }
                    promise.complete(null);
                }

                @Override
                public void operationFailed(MetaStoreException e) {
                    log.warn("[{}] Failed to update the list of ledgers after trimming", name, e);
                    ledgersListMutex.unlock();
                    trimmerMutex.unlock();

                    promise.completeExceptionally(e);
                }
            });
        }
    }

    /**
     * Delete this ManagedLedger completely from the system.
     *
     * @throws Exception
     */
    @Override
    public void delete() throws InterruptedException, ManagedLedgerException {
        final CountDownLatch counter = new CountDownLatch(1);
        final AtomicReference<ManagedLedgerException> exception = new AtomicReference<>();

        asyncDelete(new DeleteLedgerCallback() {
            @Override
            public void deleteLedgerComplete(Object ctx) {
                counter.countDown();
            }

            @Override
            public void deleteLedgerFailed(ManagedLedgerException e, Object ctx) {
                exception.set(e);
                counter.countDown();
            }

        }, null);

        if (!counter.await(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS)) {
            throw new ManagedLedgerException("Timeout during managed ledger delete operation");
        }

        if (exception.get() != null) {
            log.error("[{}] Error deleting managed ledger", name, exception.get());
            throw exception.get();
        }
    }

    @Override
    public void asyncDelete(final DeleteLedgerCallback callback, final Object ctx) {
        // Delete the managed ledger without closing, since we are not interested in gracefully closing cursors and
        // ledgers
        STATE_UPDATER.set(this, State.Fenced);

        List<ManagedCursor> cursors = Lists.newArrayList(this.cursors);
        if (cursors.isEmpty()) {
            // No cursors to delete, proceed with next step
            deleteAllLedgers(callback, ctx);
            return;
        }

        AtomicReference<ManagedLedgerException> cursorDeleteException = new AtomicReference<>();
        AtomicInteger cursorsToDelete = new AtomicInteger(cursors.size());
        for (ManagedCursor cursor : cursors) {
            asyncDeleteCursor(cursor.getName(), new DeleteCursorCallback() {
                @Override
                public void deleteCursorComplete(Object ctx) {
                    if (cursorsToDelete.decrementAndGet() == 0) {
                        if (cursorDeleteException.get() != null) {
                            // Some cursor failed to delete
                            callback.deleteLedgerFailed(cursorDeleteException.get(), ctx);
                            return;
                        }

                        // All cursors deleted, continue with deleting all ledgers
                        deleteAllLedgers(callback, ctx);
                    }
                }

                @Override
                public void deleteCursorFailed(ManagedLedgerException exception, Object ctx) {
                    log.warn("[{}] Failed to delete cursor {}", name, cursor, exception);
                    cursorDeleteException.compareAndSet(null, exception);
                    if (cursorsToDelete.decrementAndGet() == 0) {
                        // Trigger callback only once
                        callback.deleteLedgerFailed(exception, ctx);
                    }
                }
            }, null);
        }
    }

    private void asyncDeleteLedgerFromBookKeeper(long ledgerId) {
        asyncDeleteLedger(ledgerId, DEFAULT_LEDGER_DELETE_RETRIES);
    }

    private void asyncDeleteLedger(long ledgerId, LedgerInfo info) {
        if (!info.getOffloadContext().getBookkeeperDeleted()) {
            // only delete if it hasn't been previously deleted for offload
            asyncDeleteLedger(ledgerId, DEFAULT_LEDGER_DELETE_RETRIES);
        }

        if (info.getOffloadContext().hasUidMsb()) {
            UUID uuid = new UUID(info.getOffloadContext().getUidMsb(), info.getOffloadContext().getUidLsb());
            cleanupOffloaded(ledgerId, uuid,
                    OffloadUtils.getOffloadDriverName(info, config.getLedgerOffloader().getOffloadDriverName()),
                    OffloadUtils.getOffloadDriverMetadata(info, config.getLedgerOffloader().getOffloadDriverMetadata()),
                    "Trimming");
        }
    }

    private void asyncDeleteLedger(long ledgerId, long retry) {
        if (retry <= 0) {
            log.warn("[{}] Failed to delete ledger after retries {}", name, ledgerId);
            return;
        }
        bookKeeper.asyncDeleteLedger(ledgerId, (rc, ctx) -> {
            if (rc == BKException.Code.NoSuchLedgerExistsException) {
                log.warn("[{}] Ledger was already deleted {}", name, ledgerId);
            } else if (rc != BKException.Code.OK) {
                log.error("[{}] Error deleting ledger {}", name, ledgerId, BKException.getMessage(rc));
                scheduledExecutor.schedule(safeRun(() -> {
                    asyncDeleteLedger(ledgerId, retry - 1);
                }), DEFAULT_LEDGER_DELETE_BACKOFF_TIME_SEC, TimeUnit.SECONDS);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Deleted ledger {}", name, ledgerId);
                }
            }
        }, null);
    }

    @SuppressWarnings("checkstyle:fallthrough")
    private void deleteAllLedgers(DeleteLedgerCallback callback, Object ctx) {
        List<LedgerInfo> ledgers = Lists.newArrayList(ManagedLedgerImpl.this.ledgers.values());
        AtomicInteger ledgersToDelete = new AtomicInteger(ledgers.size());
        if (ledgers.isEmpty()) {
            // No ledgers to delete, proceed with deleting metadata
            deleteMetadata(callback, ctx);
            return;
        }

        for (LedgerInfo ls : ledgers) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Deleting ledger {}", name, ls);
            }
            bookKeeper.asyncDeleteLedger(ls.getLedgerId(), (rc, ctx1) -> {
                switch (rc) {
                case BKException.Code.NoSuchLedgerExistsException:
                    log.warn("[{}] Ledger {} not found when deleting it", name, ls.getLedgerId());
                    // Continue anyway

                case BKException.Code.OK:
                    if (ledgersToDelete.decrementAndGet() == 0) {
                        // All ledgers deleted, now remove ML metadata
                        deleteMetadata(callback, ctx);
                    }
                    break;

                default:
                    // Handle error
                    log.warn("[{}] Failed to delete ledger {} -- {}", name, ls.getLedgerId(),
                            BKException.getMessage(rc));
                    int toDelete = ledgersToDelete.get();
                    if (toDelete != -1 && ledgersToDelete.compareAndSet(toDelete, -1)) {
                        // Trigger callback only once
                        callback.deleteLedgerFailed(createManagedLedgerException(rc), ctx);
                    }
                }
            }, null);
        }
    }

    private void deleteMetadata(DeleteLedgerCallback callback, Object ctx) {
        store.removeManagedLedger(name, new MetaStoreCallback<Void>() {
            @Override
            public void operationComplete(Void result, Stat stat) {
                log.info("[{}] Successfully deleted managed ledger", name);
                factory.close(ManagedLedgerImpl.this);
                callback.deleteLedgerComplete(ctx);
            }

            @Override
            public void operationFailed(MetaStoreException e) {
                log.warn("[{}] Failed to delete managed ledger", name, e);
                factory.close(ManagedLedgerImpl.this);
                callback.deleteLedgerFailed(e, ctx);
            }
        });
    }

    @Override
    public Position offloadPrefix(Position pos) throws InterruptedException, ManagedLedgerException {
        CompletableFuture<Position> promise = new CompletableFuture<>();

        asyncOffloadPrefix(pos, new OffloadCallback() {
            @Override
            public void offloadComplete(Position offloadedTo, Object ctx) {
                promise.complete(offloadedTo);
            }

            @Override
            public void offloadFailed(ManagedLedgerException e, Object ctx) {
                promise.completeExceptionally(e);
            }
        }, null);

        try {
            return promise.get(AsyncOperationTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            throw new ManagedLedgerException("Timeout during managed ledger offload operation");
        } catch (ExecutionException e) {
            log.error("[{}] Error offloading. pos = {}", name, pos, e.getCause());
            throw ManagedLedgerException.getManagedLedgerException(e.getCause());
        }
    }

    @Override
    public void asyncOffloadPrefix(Position pos, OffloadCallback callback, Object ctx) {
        PositionImpl requestOffloadTo = (PositionImpl) pos;
        if (!isValidPosition(requestOffloadTo) &&
                // Also consider the case where the last ledger is currently
                // empty. In this the passed position is not technically
                // "valid", per the above check, given that it's not written
                // yes, but it will be valid for the logic here
                !(requestOffloadTo.getLedgerId() == currentLedger.getId()
                        && requestOffloadTo.getEntryId() == 0)) {
            log.warn("[{}] Cannot start offload at position {} - LastConfirmedEntry: {}", name, pos, lastConfirmedEntry);
            callback.offloadFailed(new InvalidCursorPositionException("Invalid position for offload: " + pos), ctx);
            return;
        }

        PositionImpl firstUnoffloaded;

        Queue<LedgerInfo> ledgersToOffload = new ConcurrentLinkedQueue<>();
        synchronized (this) {
            log.info("[{}] Start ledgersOffload. ledgers={} totalSize={}", name, ledgers.keySet(),
                    TOTAL_SIZE_UPDATER.get(this));

            if (STATE_UPDATER.get(this) == State.Closed) {
                log.info("[{}] Ignoring offload request since the managed ledger was already closed", name);
                callback.offloadFailed(
                        new ManagedLedgerAlreadyClosedException("Can't offload closed managed ledger (" + name + ")"),
                        ctx);
                return;
            }

            if (ledgers.isEmpty()) {
                log.info("[{}] Tried to offload a managed ledger with no ledgers, giving up", name);
                callback.offloadFailed(new ManagedLedgerAlreadyClosedException(
                        "Can't offload managed ledger (" + name + ") with no ledgers"), ctx);
                return;
            }

            long current = ledgers.lastKey();

            // the first ledger which will not be offloaded. Defaults to current,
            // in the case that the whole headmap is offloaded. Otherwise it will
            // be set as we iterate through the headmap values
            long firstLedgerRetained = current;
            for (LedgerInfo ls : ledgers.headMap(current).values()) {
                if (requestOffloadTo.getLedgerId() > ls.getLedgerId()) {
                    // don't offload if ledger has already been offloaded, or is empty
                    if (!ls.getOffloadContext().getComplete() && ls.getSize() > 0) {
                        ledgersToOffload.add(ls);
                    }
                } else {
                    firstLedgerRetained = ls.getLedgerId();
                    break;
                }
            }
            firstUnoffloaded = PositionImpl.get(firstLedgerRetained, 0);
        }

        if (ledgersToOffload.isEmpty()) {
            log.info("[{}] No ledgers to offload", name);
            callback.offloadComplete(firstUnoffloaded, ctx);
            return;
        }

        if (offloadMutex.tryLock()) {
            log.info("[{}] Going to offload ledgers {}", name,
                    ledgersToOffload.stream().map(l -> l.getLedgerId()).collect(Collectors.toList()));

            CompletableFuture<PositionImpl> promise = new CompletableFuture<>();
            promise.whenComplete((result, exception) -> {
                offloadMutex.unlock();
                if (exception != null) {
                    callback.offloadFailed(new ManagedLedgerException(exception), ctx);
                } else {
                    callback.offloadComplete(result, ctx);
                }
            });
            offloadLoop(promise, ledgersToOffload, firstUnoffloaded, Optional.empty());
        } else {
            callback.offloadFailed(
                    new ManagedLedgerException.OffloadInProgressException("Offload operation already running"), ctx);
        }
    }

    private void offloadLoop(CompletableFuture<PositionImpl> promise, Queue<LedgerInfo> ledgersToOffload,
            PositionImpl firstUnoffloaded, Optional<Throwable> firstError) {
        LedgerInfo info = ledgersToOffload.poll();
        if (info == null) {
            if (firstError.isPresent()) {
                promise.completeExceptionally(firstError.get());
            } else {
                promise.complete(firstUnoffloaded);
            }
        } else {
            long ledgerId = info.getLedgerId();
            UUID uuid = UUID.randomUUID();
            Map<String, String> extraMetadata = ImmutableMap.of("ManagedLedgerName", name);

            String driverName = config.getLedgerOffloader().getOffloadDriverName();
            Map<String, String> driverMetadata = config.getLedgerOffloader().getOffloadDriverMetadata();

            prepareLedgerInfoForOffloaded(ledgerId, uuid, driverName, driverMetadata)
                .thenCompose((ignore) -> getLedgerHandle(ledgerId))
                .thenCompose(readHandle -> config.getLedgerOffloader().offload(readHandle, uuid, extraMetadata))
                .thenCompose((ignore) -> {
                        return Retries.run(Backoff.exponentialJittered(TimeUnit.SECONDS.toMillis(1),
                                                                       TimeUnit.SECONDS.toHours(1)).limit(10),
                                           FAIL_ON_CONFLICT,
                                           () -> completeLedgerInfoForOffloaded(ledgerId, uuid),
                                           scheduledExecutor, name)
                            .whenComplete((ignore2, exception) -> {
                                    if (exception != null) {
                                        cleanupOffloaded(
                                            ledgerId, uuid,
                                            driverName, driverMetadata,
                                            "Metastore failure");
                                    }
                                });
                    })
                .whenComplete((ignore, exception) -> {
                        if (exception != null) {
                            log.warn("[{}] Exception occurred during offload", name, exception);

                            PositionImpl newFirstUnoffloaded = PositionImpl.get(ledgerId, 0);
                            if (newFirstUnoffloaded.compareTo(firstUnoffloaded) > 0) {
                                newFirstUnoffloaded = firstUnoffloaded;
                            }
                            Optional<Throwable> errorToReport = firstError;
                            synchronized (ManagedLedgerImpl.this) {
                                // if the ledger doesn't exist anymore, ignore the error
                                if (ledgers.containsKey(ledgerId)) {
                                    errorToReport = Optional.of(firstError.orElse(exception));
                                }
                            }

                            offloadLoop(promise, ledgersToOffload,
                                        newFirstUnoffloaded,
                                        errorToReport);
                        } else {
                            ledgerCache.remove(ledgerId);
                            offloadLoop(promise, ledgersToOffload, firstUnoffloaded, firstError);
                        }
                    });
        }
    }

    interface LedgerInfoTransformation {
        LedgerInfo transform(LedgerInfo oldInfo) throws ManagedLedgerException;
    }

    static Predicate<Throwable> FAIL_ON_CONFLICT = (throwable) -> {
        return !(throwable instanceof OffloadConflict) && Retries.NonFatalPredicate.test(throwable);
    };

    static class OffloadConflict extends ManagedLedgerException {
        OffloadConflict(String msg) {
            super(msg);
        }
    }

    private CompletableFuture<Void> transformLedgerInfo(long ledgerId, LedgerInfoTransformation transformation) {
        CompletableFuture<Void> promise = new CompletableFuture<Void>();

        tryTransformLedgerInfo(ledgerId, transformation, promise);

        return promise;
    }

    private void tryTransformLedgerInfo(long ledgerId, LedgerInfoTransformation transformation,
            CompletableFuture<Void> finalPromise) {
        synchronized (this) {
            if (!ledgersListMutex.tryLock()) {
                // retry in 100 milliseconds
                scheduledExecutor.schedule(
                        safeRun(() -> tryTransformLedgerInfo(ledgerId, transformation, finalPromise)), 100,
                        TimeUnit.MILLISECONDS);
            } else { // lock acquired
                CompletableFuture<Void> unlockingPromise = new CompletableFuture<>();
                unlockingPromise.whenComplete((res, ex) -> {
                    ledgersListMutex.unlock();
                    if (ex != null) {
                        finalPromise.completeExceptionally(ex);
                    } else {
                        finalPromise.complete(res);
                    }
                });

                LedgerInfo oldInfo = ledgers.get(ledgerId);
                if (oldInfo == null) {
                    unlockingPromise.completeExceptionally(new OffloadConflict(
                            "Ledger " + ledgerId + " no longer exists in ManagedLedger, likely trimmed"));
                } else {
                    try {
                        LedgerInfo newInfo = transformation.transform(oldInfo);
                        ledgers.put(ledgerId, newInfo);
                        store.asyncUpdateLedgerIds(name, getManagedLedgerInfo(), ledgersStat,
                                new MetaStoreCallback<Void>() {
                                    @Override
                                    public void operationComplete(Void result, Stat stat) {
                                        ledgersStat = stat;
                                        unlockingPromise.complete(null);
                                    }

                                    @Override
                                    public void operationFailed(MetaStoreException e) {
                                        unlockingPromise.completeExceptionally(e);
                                    }
                                });
                    } catch (ManagedLedgerException mle) {
                        unlockingPromise.completeExceptionally(mle);
                    }
                }
            }
        }
    }

    private CompletableFuture<Void> prepareLedgerInfoForOffloaded(long ledgerId, UUID uuid, String offloadDriverName,
            Map<String, String> offloadDriverMetadata) {
        log.info("[{}] Preparing metadata to offload ledger {} with uuid {}", name, ledgerId, uuid);
        return transformLedgerInfo(ledgerId,
                                   (oldInfo) -> {
                                       if (oldInfo.getOffloadContext().hasUidMsb()) {
                                           UUID oldUuid = new UUID(oldInfo.getOffloadContext().getUidMsb(),
                                                                   oldInfo.getOffloadContext().getUidLsb());
                                           log.info("[{}] Found previous offload attempt for ledger {}, uuid {}"
                                                    + ", cleaning up", name, ledgerId, uuid);
                                           cleanupOffloaded(
                                               ledgerId,
                                               oldUuid,
                                               OffloadUtils.getOffloadDriverName(oldInfo,
                                                   config.getLedgerOffloader().getOffloadDriverName()),
                                               OffloadUtils.getOffloadDriverMetadata(oldInfo,
                                                   config.getLedgerOffloader().getOffloadDriverMetadata()),
                                               "Previous failed offload");
                                       }
                                       LedgerInfo.Builder builder = oldInfo.toBuilder();
                                       builder.getOffloadContextBuilder()
                                           .setUidMsb(uuid.getMostSignificantBits())
                                           .setUidLsb(uuid.getLeastSignificantBits());
                                       OffloadUtils.setOffloadDriverMetadata(
                                           builder,
                                           offloadDriverName,
                                           offloadDriverMetadata
                                       );
                                       return builder.build();
                                   })
            .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.warn("[{}] Failed to prepare ledger {} for offload, uuid {}",
                                 name, ledgerId, uuid, exception);
                    } else {
                        log.info("[{}] Metadata prepared for offload of ledger {} with uuid {}", name, ledgerId, uuid);
                    }
                });
    }

    private CompletableFuture<Void> completeLedgerInfoForOffloaded(long ledgerId, UUID uuid) {
        log.info("[{}] Completing metadata for offload of ledger {} with uuid {}", name, ledgerId, uuid);
        return transformLedgerInfo(ledgerId,
                                   (oldInfo) -> {
                                       UUID existingUuid = new UUID(oldInfo.getOffloadContext().getUidMsb(),
                                                                    oldInfo.getOffloadContext().getUidLsb());
                                       if (existingUuid.equals(uuid)) {
                                           LedgerInfo.Builder builder = oldInfo.toBuilder();
                                           builder.getOffloadContextBuilder()
                                               .setTimestamp(clock.millis())
                                               .setComplete(true);

                                           String driverName = OffloadUtils.getOffloadDriverName(
                                               oldInfo, config.getLedgerOffloader().getOffloadDriverName());
                                           Map<String, String> driverMetadata = OffloadUtils.getOffloadDriverMetadata(
                                               oldInfo, config.getLedgerOffloader().getOffloadDriverMetadata());
                                           OffloadUtils.setOffloadDriverMetadata(
                                               builder,
                                               driverName,
                                               driverMetadata
                                           );
                                           return builder.build();
                                       } else {
                                           throw new OffloadConflict(
                                                   "Existing UUID(" + existingUuid + ") in metadata for offload"
                                                   + " of ledgerId " + ledgerId + " does not match the UUID(" + uuid
                                                   + ") for the offload we are trying to complete");
                                       }
                                   })
            .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.info("[{}] End Offload. ledger={}, uuid={}", name, ledgerId, uuid);
                    } else {
                        log.warn("[{}] Failed to complete offload of ledger {}, uuid {}",
                                 name, ledgerId, uuid, exception);
                    }
                });
    }

    private void cleanupOffloaded(long ledgerId, UUID uuid, String offloadDriverName, /*
                                                                                       * TODO: use driver name to
                                                                                       * identify offloader
                                                                                       */
            Map<String, String> offloadDriverMetadata, String cleanupReason) {
        Retries.run(Backoff.exponentialJittered(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toHours(1)).limit(10),
                Retries.NonFatalPredicate,
                () -> config.getLedgerOffloader().deleteOffloaded(ledgerId, uuid, offloadDriverMetadata),
                scheduledExecutor, name).whenComplete((ignored, exception) -> {
                    if (exception != null) {
                        log.warn("Error cleaning up offload for {}, (cleanup reason: {})", ledgerId, cleanupReason,
                                exception);
                    }
                });
    }

    /**
     * Get the number of entries between a contiguous range of two positions.
     *
     * @param range
     *            the position range
     * @return the count of entries
     */
    long getNumberOfEntries(Range<PositionImpl> range) {
        PositionImpl fromPosition = range.lowerEndpoint();
        boolean fromIncluded = range.lowerBoundType() == BoundType.CLOSED;
        PositionImpl toPosition = range.upperEndpoint();
        boolean toIncluded = range.upperBoundType() == BoundType.CLOSED;

        if (fromPosition.getLedgerId() == toPosition.getLedgerId()) {
            // If the 2 positions are in the same ledger
            long count = toPosition.getEntryId() - fromPosition.getEntryId() - 1;
            count += fromIncluded ? 1 : 0;
            count += toIncluded ? 1 : 0;
            return count;
        } else {
            long count = 0;
            // If the from & to are pointing to different ledgers, then we need to :
            // 1. Add the entries in the ledger pointed by toPosition
            count += toPosition.getEntryId();
            count += toIncluded ? 1 : 0;

            // 2. Add the entries in the ledger pointed by fromPosition
            LedgerInfo li = ledgers.get(fromPosition.getLedgerId());
            if (li != null) {
                count += li.getEntries() - (fromPosition.getEntryId() + 1);
                count += fromIncluded ? 1 : 0;
            }

            // 3. Add the whole ledgers entries in between
            for (LedgerInfo ls : ledgers.subMap(fromPosition.getLedgerId(), false, toPosition.getLedgerId(), false)
                    .values()) {
                count += ls.getEntries();
            }

            return count;
        }
    }

    /**
     * Get the entry position at a given distance from a given position.
     *
     * @param startPosition
     *            starting position
     * @param n
     *            number of entries to skip ahead
     * @param startRange
     *            specifies whether or not to include the start position in calculating the distance
     * @return the new position that is n entries ahead
     */
    PositionImpl getPositionAfterN(final PositionImpl startPosition, long n, PositionBound startRange) {
        long entriesToSkip = n;
        long currentLedgerId;
        long currentEntryId;

        if (startRange == PositionBound.startIncluded) {
            currentLedgerId = startPosition.getLedgerId();
            currentEntryId = startPosition.getEntryId();
        } else {
            // e.g. a mark-delete position
            PositionImpl nextValidPosition = getNextValidPosition(startPosition);
            currentLedgerId = nextValidPosition.getLedgerId();
            currentEntryId = nextValidPosition.getEntryId();
        }

        boolean lastLedger = false;
        long totalEntriesInCurrentLedger;

        while (entriesToSkip >= 0) {
            // for the current ledger, the number of entries written is deduced from the lastConfirmedEntry
            // for previous ledgers, LedgerInfo in ZK has the number of entries
            if (currentLedger != null && currentLedgerId == currentLedger.getId()) {
                lastLedger = true;
                totalEntriesInCurrentLedger = lastConfirmedEntry.getEntryId() + 1;
            } else {
                totalEntriesInCurrentLedger = ledgers.get(currentLedgerId).getEntries();
            }

            long unreadEntriesInCurrentLedger = totalEntriesInCurrentLedger - currentEntryId;

            if (unreadEntriesInCurrentLedger >= entriesToSkip) {
                // if the current ledger has more entries than what we need to skip
                // then the return position is in the same ledger
                currentEntryId += entriesToSkip;
                break;
            } else {
                // skip remaining entry from the next ledger
                entriesToSkip -= unreadEntriesInCurrentLedger;
                if (lastLedger) {
                    // there are no more ledgers, return the last position
                    currentEntryId = totalEntriesInCurrentLedger;
                    break;
                } else {
                    Long lid = ledgers.ceilingKey(currentLedgerId + 1);
                    currentLedgerId = lid != null ? lid : (ledgers.lastKey() + 1);
                    currentEntryId = 0;
                }
            }
        }

        PositionImpl positionToReturn = getPreviousPosition(PositionImpl.get(currentLedgerId, currentEntryId));
        if (log.isDebugEnabled()) {
            log.debug("getPositionAfterN: Start position {}:{}, startIncluded: {}, Return position {}:{}",
                    startPosition.getLedgerId(), startPosition.getEntryId(), startRange, positionToReturn.getLedgerId(),
                    positionToReturn.getEntryId());
        }

        return positionToReturn;
    }

    /**
     * Get the entry position that come before the specified position in the message stream, using information from the
     * ledger list and each ledger entries count.
     *
     * @param position
     *            the current position
     * @return the previous position
     */
    PositionImpl getPreviousPosition(PositionImpl position) {
        if (position.getEntryId() > 0) {
            return PositionImpl.get(position.getLedgerId(), position.getEntryId() - 1);
        }

        // The previous position will be the last position of an earlier ledgers
        NavigableMap<Long, LedgerInfo> headMap = ledgers.headMap(position.getLedgerId(), false);

        if (headMap.isEmpty()) {
            // There is no previous ledger, return an invalid position in the current ledger
            return PositionImpl.get(position.getLedgerId(), -1);
        }

        // We need to find the most recent non-empty ledger
        for (long ledgerId : headMap.descendingKeySet()) {
            LedgerInfo li = headMap.get(ledgerId);
            if (li.getEntries() > 0) {
                return PositionImpl.get(li.getLedgerId(), li.getEntries() - 1);
            }
        }

        // in case there are only empty ledgers, we return a position in the first one
        return PositionImpl.get(headMap.firstEntry().getKey(), -1);
    }

    /**
     * Validate whether a specified position is valid for the current managed ledger.
     *
     * @param position
     *            the position to validate
     * @return true if the position is valid, false otherwise
     */
    boolean isValidPosition(PositionImpl position) {
        PositionImpl last = lastConfirmedEntry;
        if (log.isDebugEnabled()) {
            log.debug("IsValid position: {} -- last: {}", position, last);
        }

        if (position.getEntryId() < 0) {
            return false;
        } else if (position.getLedgerId() > last.getLedgerId()) {
            return false;
        } else if (position.getLedgerId() == last.getLedgerId()) {
            return position.getEntryId() <= (last.getEntryId() + 1);
        } else {
            // Look in the ledgers map
            LedgerInfo ls = ledgers.get(position.getLedgerId());

            if (ls == null) {
                if (position.getLedgerId() < last.getLedgerId()) {
                    // Pointing to a non existing ledger that is older than the current ledger is invalid
                    return false;
                } else {
                    // Pointing to a non existing ledger is only legitimate if the ledger was empty
                    return position.getEntryId() == 0;
                }
            }

            return position.getEntryId() < ls.getEntries();
        }
    }

    boolean ledgerExists(long ledgerId) {
        return ledgers.get(ledgerId) != null;
    }

    Long getNextValidLedger(long ledgerId) {
        return ledgers.ceilingKey(ledgerId + 1);
    }

    PositionImpl getNextValidPosition(final PositionImpl position) {
        PositionImpl nextPosition = position.getNext();
        while (!isValidPosition(nextPosition)) {
            Long nextLedgerId = ledgers.ceilingKey(nextPosition.getLedgerId() + 1);
            if (nextLedgerId == null) {
                return null;
            }
            nextPosition = PositionImpl.get(nextLedgerId.longValue(), 0);
        }
        return nextPosition;
    }

    PositionImpl getFirstPosition() {
        Long ledgerId = ledgers.firstKey();
        if (ledgerId == null) {
            return null;
        }
        if (ledgerId > lastConfirmedEntry.getLedgerId()) {
            checkState(ledgers.get(ledgerId).getEntries() == 0);
            ledgerId = lastConfirmedEntry.getLedgerId();
        }
        return new PositionImpl(ledgerId, -1);
    }

    PositionImpl getLastPosition() {
        return lastConfirmedEntry;
    }

    @Override
    public ManagedCursor getSlowestConsumer() {
        return cursors.getSlowestReader();
    }

    PositionImpl getMarkDeletePositionOfSlowestConsumer() {
        ManagedCursor slowestCursor = getSlowestConsumer();
        return slowestCursor == null ? null : (PositionImpl) slowestCursor.getMarkDeletedPosition();
    }

    /**
     * Get the last position written in the managed ledger, alongside with the associated counter.
     */
    Pair<PositionImpl, Long> getLastPositionAndCounter() {
        PositionImpl pos;
        long count;

        do {
            pos = lastConfirmedEntry;
            count = ENTRIES_ADDED_COUNTER_UPDATER.get(this);

            // Ensure no entry was written while reading the two values
        } while (pos.compareTo(lastConfirmedEntry) != 0);

        return Pair.of(pos, count);
    }

    /**
     * Get the first position written in the managed ledger, alongside with the associated counter
     */
    Pair<PositionImpl, Long> getFirstPositionAndCounter() {
        PositionImpl pos;
        long count;
        Pair<PositionImpl, Long> lastPositionAndCounter;

        do {
            pos = getFirstPosition();
            lastPositionAndCounter = getLastPositionAndCounter();
            count = lastPositionAndCounter.getRight()
                    - getNumberOfEntries(Range.openClosed(pos, lastPositionAndCounter.getLeft()));
        } while (pos.compareTo(getFirstPosition()) != 0
                || lastPositionAndCounter.getLeft().compareTo(getLastPosition()) != 0);
        return Pair.of(pos, count);
    }

    public void activateCursor(ManagedCursor cursor) {
        if (activeCursors.get(cursor.getName()) == null) {
            activeCursors.add(cursor);
        }
    }

    public void deactivateCursor(ManagedCursor cursor) {
        synchronized (activeCursors) {
            if (activeCursors.get(cursor.getName()) != null) {
                activeCursors.removeCursor(cursor.getName());
                if (activeCursors.isEmpty()) {
                    // cleanup cache if there is no active subscription
                    entryCache.clear();
                } else {
                    // if removed subscription was the slowest subscription : update cursor and let it clear cache:
                    // till new slowest-cursor's read-position
                    discardEntriesFromCache((ManagedCursorImpl) activeCursors.getSlowestReader(),
                            getPreviousPosition((PositionImpl) activeCursors.getSlowestReader().getReadPosition()));
                }
            }
        }
    }

    public boolean isCursorActive(ManagedCursor cursor) {
        return cursor.isDurable() && activeCursors.get(cursor.getName()) != null;
    }

    private boolean currentLedgerIsFull() {
        boolean spaceQuotaReached = (currentLedgerEntries >= config.getMaxEntriesPerLedger()
                || currentLedgerSize >= (config.getMaxSizePerLedgerMb() * MegaByte));

        long timeSinceLedgerCreationMs = clock.millis() - lastLedgerCreatedTimestamp;
        boolean maxLedgerTimeReached = timeSinceLedgerCreationMs >= maximumRolloverTimeMs;

        if (spaceQuotaReached || maxLedgerTimeReached) {
            if (config.getMinimumRolloverTimeMs() > 0) {

                boolean switchLedger = timeSinceLedgerCreationMs > config.getMinimumRolloverTimeMs();
                if (log.isDebugEnabled()) {
                    log.debug("Diff: {}, threshold: {} -- switch: {}", clock.millis() - lastLedgerCreatedTimestamp,
                            config.getMinimumRolloverTimeMs(), switchLedger);
                }
                return switchLedger;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public List<LedgerInfo> getLedgersInfoAsList() {
        return Lists.newArrayList(ledgers.values());
    }

    public NavigableMap<Long, LedgerInfo> getLedgersInfo() {
        return ledgers;
    }

    OrderedScheduler getScheduledExecutor() {
        return scheduledExecutor;
    }

    OrderedExecutor getExecutor() {
        return executor;
    }

    private ManagedLedgerInfo getManagedLedgerInfo() {
        ManagedLedgerInfo.Builder mlInfo = ManagedLedgerInfo.newBuilder().addAllLedgerInfo(ledgers.values());
        if (state == State.Terminated) {
            mlInfo.setTerminatedPosition(NestedPositionInfo.newBuilder().setLedgerId(lastConfirmedEntry.getLedgerId())
                    .setEntryId(lastConfirmedEntry.getEntryId()));
        }

        return mlInfo.build();
    }

    /**
     * Throws an exception if the managed ledger has been previously fenced.
     *
     * @throws ManagedLedgerException
     */
    private void checkFenced() throws ManagedLedgerException {
        if (STATE_UPDATER.get(this) == State.Fenced) {
            log.error("[{}] Attempted to use a fenced managed ledger", name);
            throw new ManagedLedgerFencedException();
        }
    }

    private void checkManagedLedgerIsOpen() throws ManagedLedgerException {
        if (STATE_UPDATER.get(this) == State.Closed) {
            throw new ManagedLedgerException("ManagedLedger " + name + " has already been closed");
        }
    }

    synchronized void setFenced() {
        STATE_UPDATER.set(this, State.Fenced);
    }

    MetaStore getStore() {
        return store;
    }

    @Override
    public ManagedLedgerConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(ManagedLedgerConfig config) {
        this.config = config;
        this.cursors.forEach(c -> c.setThrottleMarkDelete(config.getThrottleMarkDelete()));
    }

    interface ManagedLedgerInitializeLedgerCallback {
        void initializeComplete();

        void initializeFailed(ManagedLedgerException e);
    }

    // Expose internal values for debugging purposes
    public long getEntriesAddedCounter() {
        return ENTRIES_ADDED_COUNTER_UPDATER.get(this);
    }

    public long getCurrentLedgerEntries() {
        return currentLedgerEntries;
    }

    public long getCurrentLedgerSize() {
        return currentLedgerSize;
    }

    public long getLastLedgerCreatedTimestamp() {
        return lastLedgerCreatedTimestamp;
    }

    public long getLastLedgerCreationFailureTimestamp() {
        return lastLedgerCreationFailureTimestamp;
    }

    public int getWaitingCursorsCount() {
        return waitingCursors.size();
    }

    public int getPendingAddEntriesCount() {
        return pendingAddEntries.size();
    }

    @Override
    public Position getLastConfirmedEntry() {
        return lastConfirmedEntry;
    }

    public String getState() {
        return STATE_UPDATER.get(this).toString();
    }

    public ManagedLedgerMBeanImpl getMBean() {
        return mbean;
    }

    public long getCacheSize() {
        return entryCache.getSize();
    }

    protected boolean isReadOnly() {
        // Default managed ledger implementation is read-write
        return false;
    }

    /**
     * return BK error codes that are considered not likely to be recoverable.
     */
    private static boolean isBkErrorNotRecoverable(int rc) {
        switch (rc) {
        case BKException.Code.NoSuchLedgerExistsException:
        case BKException.Code.NoSuchEntryException:
            return true;

        default:
            return false;
        }
    }

    public static ManagedLedgerException createManagedLedgerException(int bkErrorCode) {
        if (bkErrorCode == BKException.Code.TooManyRequestsException) {
            return new TooManyRequestsException("Too many request error from bookies");
        } else if (isBkErrorNotRecoverable(bkErrorCode)) {
            return new NonRecoverableLedgerException(BKException.getMessage(bkErrorCode));
        } else {
            return new ManagedLedgerException(BKException.getMessage(bkErrorCode));
        }
    }

    public static ManagedLedgerException createManagedLedgerException(Throwable t) {
        if (t instanceof org.apache.bookkeeper.client.api.BKException) {
            return createManagedLedgerException(((org.apache.bookkeeper.client.api.BKException) t).getCode());
        } else {
            return new ManagedLedgerException("Unknown exception");
        }
    }

    /**
     * Create ledger async and schedule a timeout task to check ledger-creation is complete else it fails the callback
     * with TimeoutException.
     *
     * @param bookKeeper
     * @param config
     * @param digestType
     * @param cb
     * @param metadata
     */
    protected void asyncCreateLedger(BookKeeper bookKeeper, ManagedLedgerConfig config, DigestType digestType,
            CreateCallback cb, Map<String, byte[]> metadata) {
        AtomicBoolean ledgerCreated = new AtomicBoolean(false);
        Map<String, byte[]> finalMetadata = new HashMap<>();
        finalMetadata.putAll(ledgerMetadata);
        finalMetadata.putAll(metadata);
        if (log.isDebugEnabled()) {
            log.debug("creating ledger, metadata: "+finalMetadata);
        }
        bookKeeper.asyncCreateLedger(config.getEnsembleSize(), config.getWriteQuorumSize(), config.getAckQuorumSize(),
                digestType, config.getPassword(), cb, ledgerCreated, finalMetadata);
        scheduledExecutor.schedule(() -> {
            if (!ledgerCreated.get()) {
                ledgerCreated.set(true);
                cb.createComplete(BKException.Code.TimeoutException, null, null);
            }
        }, config.getMetadataOperationsTimeoutSeconds(), TimeUnit.SECONDS);
    }

    /**
     * check if ledger-op task is already completed by timeout-task. If completed then delete the created ledger
     *
     * @param rc
     * @param lh
     * @param ctx
     * @return
     */
    protected boolean checkAndCompleteLedgerOpTask(int rc, LedgerHandle lh, Object ctx) {
        if (ctx != null && ctx instanceof AtomicBoolean) {
            // ledger-creation is already timed out and callback is already completed so, delete this ledger and return.
            if (((AtomicBoolean) (ctx)).get()) {
                if (rc == BKException.Code.OK) {
                    log.warn("[{}]-{} ledger creation timed-out, deleting ledger", this.name, lh.getId());
                    asyncDeleteLedger(lh.getId(), DEFAULT_LEDGER_DELETE_RETRIES);
                }
                return true;
            }
            ((AtomicBoolean) ctx).set(true);
        }
        return false;
    }

    private void scheduleTimeoutTask() {
        // disable timeout task checker if timeout <= 0
        if (config.getAddEntryTimeoutSeconds() > 0 || config.getReadEntryTimeoutSeconds() > 0) {
            long timeoutSec = Math.min(config.getAddEntryTimeoutSeconds(), config.getReadEntryTimeoutSeconds());
            timeoutSec = timeoutSec <= 0
                    ? Math.max(config.getAddEntryTimeoutSeconds(), config.getReadEntryTimeoutSeconds())
                    : timeoutSec;
            this.timeoutTask = this.scheduledExecutor.scheduleAtFixedRate(safeRun(() -> {
                checkAddTimeout();
                checkReadTimeout();
            }), timeoutSec, timeoutSec, TimeUnit.SECONDS);
        }
    }

    private void checkAddTimeout() {
        long timeoutSec = config.getAddEntryTimeoutSeconds();
        if (timeoutSec < 1) {
            return;
        }
        OpAddEntry opAddEntry = pendingAddEntries.peek();
        if (opAddEntry != null) {
            boolean isTimedOut = opAddEntry.lastInitTime != -1
                    && TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - opAddEntry.lastInitTime) >= timeoutSec;
            if (isTimedOut) {
                log.error("Failed to add entry for ledger {} in time-out {} sec",
                        (opAddEntry.ledger != null ? opAddEntry.ledger.getId() : -1), timeoutSec);
                opAddEntry.handleAddTimeoutFailure(opAddEntry.ledger, opAddEntry.addOpCount);
            }
        }
    }

    private void checkReadTimeout() {
        long timeoutSec = config.getReadEntryTimeoutSeconds();
        if (timeoutSec < 1) {
            return;
        }
        ReadEntryCallbackWrapper callback = this.lastReadCallback;
        long readOpCount = callback != null ? callback.readOpCount : 0;
        boolean timeout = callback != null
                ? (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - callback.createdTime) >= timeoutSec)
                : false;
        if (readOpCount > 0 && callback != null && timeout) {
            log.warn("[{}]-{}-{} read entry timeout after {} sec", this.name, this.lastReadCallback.ledgerId,
                    this.lastReadCallback.entryId, timeoutSec);
            callback.readFailed(createManagedLedgerException(BKException.Code.TimeoutException), readOpCount);
            LAST_READ_CALLBACK_UPDATER.compareAndSet(this, callback, null);
        }
    }

    @Override
    public long getOffloadedSize() {
        long offloadedSize = 0;
        for (LedgerInfo li : ledgers.values()) {
            if (li.hasOffloadContext() && li.getOffloadContext().getComplete()) {
                offloadedSize += li.getSize();
            }
        }

        return offloadedSize;
    }

    private static final Logger log = LoggerFactory.getLogger(ManagedLedgerImpl.class);

}
