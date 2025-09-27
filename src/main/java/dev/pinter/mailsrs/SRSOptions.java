/*
 * Copyright 2025 Emerson Pinter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.pinter.mailsrs;

import java.time.Duration;

/**
 * Configuration for SRS constructor
 *
 * <pre>{@code
 * lifetime:      Maximum age (days) a timestamp can be considered valid
 * alwaysRewrite: Always rewrite when calling forward, even if forwarder(alias) is equal to the domain part
 * separator:     The separator to use after SRS0 and SRS1, default is '=', possible options are '+' and '-'
 * hashLength:    Hash length for created SRS addresses
 * hashMin:       Minimum hash length considered valid
 * }</pre>
 */
public class SRSOptions {
    private final Duration lifetime;
    private final boolean alwaysRewrite;
    private final char separator;
    private final int hashLength;
    private final int hashMin;
    private final boolean tryVerifySrs1Time;
    private final boolean disableTimestampValidation;

    private SRSOptions(Builder builder) {
        if (!Character.toString(builder.separator).matches("^[=\\-+]$")) {
            builder.separator = '=';
        }
        this.alwaysRewrite = builder.alwaysRewrite;
        this.separator = builder.separator;
        this.hashLength = builder.hashLength;
        this.hashMin = builder.hashMin;
        this.lifetime = builder.lifetime;
        this.tryVerifySrs1Time = builder.tryVerifySrs1Time;
        this.disableTimestampValidation = builder.disableTimestampValidation;
    }

    Duration getLifetime() {
        return lifetime;
    }

    boolean isAlwaysRewrite() {
        return alwaysRewrite;
    }

    char getSeparator() {
        return separator;
    }

    int getHashLength() {
        return hashLength;
    }

    int getHashMin() {
        return hashMin;
    }

    boolean isTryVerifySrs1Time() {
        return tryVerifySrs1Time;
    }

    boolean isDisableTimestampValidation() {
        return disableTimestampValidation;
    }

    @SuppressWarnings("unused")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for {@link SRSOptions}
     */
    @SuppressWarnings("unused")
    public static class Builder {
        private Duration lifetime = Duration.ofDays(30);
        private boolean alwaysRewrite = false;
        private boolean tryVerifySrs1Time = false;
        private char separator = '=';
        private int hashLength = 4;
        private int hashMin = 4;
        private boolean disableTimestampValidation = false;

        /**
         * Maximum age (days) a timestamp can be considered valid. Default 30 days. Minimum is 1 day, time less than
         * 1 day is ignored.
         *
         * @param lifetime A {@link Duration} instance representing the max-age, at least 1 day
         * @return {@link Builder}
         */
        public Builder withLifetime(Duration lifetime) {
            if (lifetime.compareTo(Duration.ofDays(1)) < 0) {
                return this;
            }
            this.lifetime = lifetime;
            return this;
        }

        /**
         * Always rewrite when calling forward, even if forwarder(alias) is equal to the domain part. Default is false.
         * It is strongly recommended to keep this disabled.
         *
         * @param alwaysRewrite     If true, the address will always be rewritten, even if it is the same domain.
         * @return {@link Builder}
         */
        public Builder withAlwaysRewrite(boolean alwaysRewrite) {
            this.alwaysRewrite = alwaysRewrite;
            return this;
        }

        /**
         * The separator to use after SRS0 and SRS1, possible options are '=', '+' and '-'. Default is '='.
         *
         * @param separator     A character, can be '=', '+' or '-'.
         * @return {@link Builder}
         */
        public Builder withSeparator(char separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Hash length for created SRS addresses. Default and recommended is 4.
         *
         * @param hashLength    Hash length, default is 4
         * @return {@link Builder}
         */
        public Builder withHashLength(int hashLength) {
            this.hashLength = hashLength;
            return this;
        }

        /**
         * Minimum hash length considered valid. Default and recommended is 4.
         *
         * @param hashMin    Minimum hash length, default 4.
         * @return {@link Builder}
         */
        public Builder withHashMin(int hashMin) {
            this.hashMin = hashMin;
            return this;
        }

        /**
         * When true, a timestamp validation will try to be done if timestamp of an SRS1 was successfully parsed.
         * It is recommended to be kept disabled, only set this to true if you know what you are doing.
         * The SRS proposal says SRS1 timestamp verification is not needed, and the reference implementation doesn't
         * verify it. Default is disabled.
         *
         * @param tryVerifySrs1Time    When true,
         *                             a timestamp validation will try to be done if timestamp of an SRS1 was
         *                             successfully parsed.
         * @return {@link Builder}
         */
        public Builder withTryVerifySrs1Time(boolean tryVerifySrs1Time) {
            this.tryVerifySrs1Time = tryVerifySrs1Time;
            return this;
        }

        /**
         * When true, disables timestamp validation. This should never be used in production environments, unless
         * you know what you are doing. Default is always validate timestamp (except for SRS1, as is specified by the
         * SRS proposal).
         *
         * @param disableTimestampValidation    When true, skip timestamp validation.
         * @return {@link Builder}
         */
        public Builder withDisableTimestampValidation(boolean disableTimestampValidation) {
            this.disableTimestampValidation = disableTimestampValidation;
            return this;
        }

        public SRSOptions build() {
            return new SRSOptions(this);
        }
    }
}