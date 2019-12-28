/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.common;

import static java.util.concurrent.Executors.defaultThreadFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A builder for {@link ThreadFactory} instances. This builder is intended to be used for creating thread factories to
 * be used, e.g., when creating {@link Executor}s via the {@link Executors} utility methods.
 * <p>
 * The built {@link ThreadFactory} uses a wrapped {@link ThreadFactory} to create threads (defaulting to
 * {@link Executors#defaultThreadFactory()}, and then overwrites thread properties as indicated in the build process.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public class ThreadFactoryBuilder {

    private static final String DEFAULT_NAME_PREFIX = "ESH";

    private @Nullable ThreadFactory wrappedThreadFactory;
    private @Nullable String name;
    private @Nullable String namePrefix = DEFAULT_NAME_PREFIX;
    private boolean daemonThreads;
    private @Nullable UncaughtExceptionHandler uncaughtExceptionHandler;
    private @Nullable Integer priority;

    /**
     * Creates a fresh {@link ThreadFactoryBuilder}.
     *
     * @return A {@link ThreadFactoryBuilder}
     */
    public static ThreadFactoryBuilder create() {
        return new ThreadFactoryBuilder();
    }

    private ThreadFactoryBuilder() {
        // use static factory method to create ThreadFactoryBuilder
    }

    /**
     * Sets the wrapped thread factory used to create threads.
     * <p>
     * If set to null, {@link Executors#defaultThreadFactory()} is used.
     * <p>
     * Defaults to null.
     *
     * @param wrappedThreadFactory the wrapped thread factory to be used
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withWrappedThreadFactory(@Nullable ThreadFactory wrappedThreadFactory) {
        this.wrappedThreadFactory = wrappedThreadFactory;
        return this;
    }

    /**
     * Sets the name to be used by the {@link ThreadFactory}.
     * <p>
     * The threads created by the {@link ThreadFactory} are named 'namePrefix-name-i', where i is an integer
     * incremented with each new thread, initialized to 1.
     * <p>
     * If the name is set to null, the created threads are named 'namePrefix-generatedName', where 'generatedName' is
     * the name generated by the wrapped thread factory.
     * <p>
     * Defaults to null.
     *
     * @param name The name (can be null)
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withName(@Nullable String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the name prefix to be used by the {@link ThreadFactory}.
     * <p>
     * The threads created by the {@link ThreadFactory} are named 'namePrefix-name-i', where i is an integer
     * incremented with each new thread, initialized to 1. If set to null, threads are named 'name-i'.
     * <p>
     * Defaults to the name prefix 'ESH'. Setting a name prefix different than the default one is intended to be used by
     * solutions integrating openHAB.
     *
     * @param namePrefix The name prefix (can be null)
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withNamePrefix(@Nullable String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    /**
     * Sets whether the {@link ThreadFactory} will create daemon threads. Defaults to false.
     *
     * @param daemonThreads indicates whether daemon threads shall be created
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withDaemonThreads(boolean daemonThreads) {
        this.daemonThreads = daemonThreads;
        return this;
    }

    /**
     * Sets the {@link Thread.UncaughtExceptionHandler} to be used for created threads. If set to null, the built
     * {@link ThreadFactory} will not change the handler set by the wrapped {@link ThreadFactory}. Defaults to null.
     *
     * @param uncaughtExceptionHandler The {@link Thread.UncaughtExceptionHandler} to be use for created threads.
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withUncaughtExceptionHandler(
            @Nullable UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * Sets the priority to be set for created threads. Must be a valid thread priority, as indicated by
     * {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}. If set to null, the built {@link ThreadFactory} will
     * not change the priority set by the wrapped {@link ThreadFactory}. Defaults to null.
     *
     * @param priority The priority to be used for created threads.
     * @return this {@link ThreadFactoryBuilder} instance
     */
    public ThreadFactoryBuilder withPriority(@Nullable Integer priority) {
        if (priority != null && priority.intValue() < Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                    "The provided priority %d is below the minimal thread priority %d", priority, Thread.MIN_PRIORITY));
        }

        if (priority != null && priority.intValue() > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                    "The provided priority %d is above the maximal thread priority %d", priority, Thread.MAX_PRIORITY));
        }

        this.priority = priority;
        return this;
    }

    /**
     * Builds the {@link ThreadFactory}, configuring it as specified.
     *
     * @return the {@link ThreadFactory}
     */
    public ThreadFactory build() {
        ThreadFactory wrappedThreadFactory = this.wrappedThreadFactory;
        if (wrappedThreadFactory == null) {
            wrappedThreadFactory = defaultThreadFactory();
        }

        return ThreadFactoryBuilder.build(wrappedThreadFactory, namePrefix, name, daemonThreads,
                uncaughtExceptionHandler, priority);
    }

    private static ThreadFactory build(ThreadFactory wrappedThreadFactory, @Nullable String namePrefix,
            @Nullable String name, boolean daemonThreads, @Nullable UncaughtExceptionHandler uncaughtExceptionHandler,
            @Nullable Integer priority) {
        return new ThreadFactory() {
            AtomicInteger threadCounter = new AtomicInteger(1);

            @Override
            public Thread newThread(@Nullable Runnable runnable) {
                Thread thread = wrappedThreadFactory.newThread(runnable);

                if (namePrefix != null && name != null) {
                    thread.setName(String.format("%s-%s-%d", namePrefix, name, threadCounter.getAndIncrement()));
                } else if (namePrefix != null && name == null) {
                    thread.setName(String.format("%s-%s", namePrefix, thread.getName()));
                } else if (namePrefix == null && name != null) {
                    thread.setName(String.format("%s-%d", name, threadCounter.getAndIncrement()));
                }

                thread.setDaemon(daemonThreads);

                if (uncaughtExceptionHandler != null) {
                    thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }

                if (priority != null) {
                    thread.setPriority(priority);
                }

                return thread;
            }
        };
    }

}