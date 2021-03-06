/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReactiveRedisConnection.BooleanResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.ByteBufferResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.Command;
import org.springframework.data.redis.connection.ReactiveRedisConnection.CommandResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.KeyCommand;
import org.springframework.data.redis.connection.ReactiveRedisConnection.MultiValueResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.NumericResponse;
import org.springframework.data.redis.connection.ReactiveRedisConnection.RangeCommand;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;

/**
 * Redis String commands executed using reactive infrastructure.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.0
 */
public interface ReactiveStringCommands {

	/**
	 * {@code SET} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/set">Redis Documentation: SET</a>
	 */
	class SetCommand extends KeyCommand {

		private ByteBuffer value;
		private Expiration expiration;
		private SetOption option;

		private SetCommand(ByteBuffer key, ByteBuffer value, Expiration expiration, SetOption option) {

			super(key);

			this.value = value;
			this.expiration = expiration;
			this.option = option;
		}

		/**
		 * Creates a new {@link SetCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link SetCommand} for a {@literal key}.
		 */
		public static SetCommand set(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new SetCommand(key, null, null, null);
		}

		/**
		 * Applies the {@literal value}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param value must not be {@literal null}.
		 * @return a new {@link SetCommand} with {@literal value} applied.
		 */
		public SetCommand value(ByteBuffer value) {

			Assert.notNull(value, "Value must not be null!");

			return new SetCommand(getKey(), value, expiration, option);
		}

		/**
		 * Applies {@link Expiration}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param expiration must not be {@literal null}.
		 * @return a new {@link SetCommand} with {@link Expiration} applied.
		 */
		public SetCommand expiring(Expiration expiration) {

			Assert.notNull(expiration, "Expiration must not be null!");

			return new SetCommand(getKey(), value, expiration, option);
		}

		/**
		 * Applies {@link SetOption}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param option must not be {@literal null}.
		 * @return a new {@link SetCommand} with {@link SetOption} applied.
		 */
		public SetCommand withSetOption(SetOption option) {

			Assert.notNull(option, "SetOption must not be null!");

			return new SetCommand(getKey(), value, expiration, option);
		}

		/**
		 * @return
		 */
		public ByteBuffer getValue() {
			return value;
		}

		/**
		 * @return
		 */
		public Optional<Expiration> getExpiration() {
			return Optional.ofNullable(expiration);
		}

		/**
		 * @return
		 */
		public Optional<SetOption> getOption() {
			return Optional.ofNullable(option);
		}
	}

	/**
	 * Set {@literal value} for {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/set">Redis Documentation: SET</a>
	 */
	default Mono<Boolean> set(ByteBuffer key, ByteBuffer value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return set(Mono.just(SetCommand.set(key).value(value))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Set {@literal value} for {@literal key} with {@literal expiration} and {@literal options}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param expiration must not be {@literal null}.
	 * @param option must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/set">Redis Documentation: SET</a>
	 */
	default Mono<Boolean> set(ByteBuffer key, ByteBuffer value, Expiration expiration, SetOption option) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return set(Mono.just(SetCommand.set(key).value(value).withSetOption(option).expiring(expiration))).next()
				.map(BooleanResponse::getOutput);
	}

	/**
	 * Set each and every item separately by invoking {@link SetCommand}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return {@link Flux} of {@link BooleanResponse} holding the {@link SetCommand} along with the command result.
	 * @see <a href="http://redis.io/commands/set">Redis Documentation: SET</a>
	 */
	Flux<BooleanResponse<SetCommand>> set(Publisher<SetCommand> commands);

	/**
	 * Get single element stored at {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return empty {@link ByteBuffer} in case {@literal key} does not exist.
	 * @see <a href="http://redis.io/commands/get">Redis Documentation: GET</a>
	 */
	default Mono<ByteBuffer> get(ByteBuffer key) {

		Assert.notNull(key, "Key must not be null!");

		return get(Mono.just(new KeyCommand(key))).next().map(CommandResponse::getOutput);
	}

	/**
	 * Get elements one by one.
	 *
	 * @param keys must not be {@literal null}.
	 * @return {@link Flux} of {@link ByteBufferResponse} holding the {@literal key} to get along with the value
	 *         retrieved.
	 * @see <a href="http://redis.io/commands/get">Redis Documentation: GET</a>
	 */
	Flux<ByteBufferResponse<KeyCommand>> get(Publisher<KeyCommand> keys);

	/**
	 * Set {@literal value} for {@literal key} and return the existing value.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/getset">Redis Documentation: GETSET</a>
	 */
	default Mono<ByteBuffer> getSet(ByteBuffer key, ByteBuffer value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return getSet(Mono.just(SetCommand.set(key).value(value))).next().map(ByteBufferResponse::getOutput);
	}

	/**
	 * Set {@literal value} for {@literal key} and return the existing value one by one.
	 *
	 * @param commands must not be {@literal null}.
	 * @return {@link Flux} of {@link ByteBufferResponse} holding the {@link SetCommand} along with the previously
	 *         existing value.
	 * @see <a href="http://redis.io/commands/getset">Redis Documentation: GETSET</a>
	 */
	Flux<ByteBufferResponse<SetCommand>> getSet(Publisher<SetCommand> commands);

	/**
	 * Get multiple values in one batch.
	 *
	 * @param keys must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/mget">Redis Documentation: MGET</a>
	 */
	default Mono<List<ByteBuffer>> mGet(List<ByteBuffer> keys) {

		Assert.notNull(keys, "Keys must not be null!");

		return mGet(Mono.just(keys)).next().map(MultiValueResponse::getOutput);
	}

	/**
	 * Get multiple values at for {@literal keysets} in batches.
	 *
	 * @param keysets must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/mget">Redis Documentation: MGET</a>
	 */
	Flux<MultiValueResponse<List<ByteBuffer>, ByteBuffer>> mGet(Publisher<List<ByteBuffer>> keysets);

	/**
	 * Set {@literal value} for {@literal key}, only if {@literal key} does not exist.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setnx">Redis Documentation: SETNX</a>
	 */
	default Mono<Boolean> setNX(ByteBuffer key, ByteBuffer value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return setNX(Mono.just(SetCommand.set(key).value(value))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Set {@literal key value} pairs, only if {@literal key} does not exist.
	 *
	 * @param values must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setnx">Redis Documentation: SETNX</a>
	 */
	Flux<BooleanResponse<SetCommand>> setNX(Publisher<SetCommand> values);

	/**
	 * Set {@literal key value} pair and {@link Expiration}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param expireTimeout must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setex">Redis Documentation: SETEX</a>
	 */
	default Mono<Boolean> setEX(ByteBuffer key, ByteBuffer value, Expiration expireTimeout) {

		Assert.notNull(key, "Keys must not be null!");
		Assert.notNull(value, "Keys must not be null!");
		Assert.notNull(expireTimeout, "ExpireTimeout must not be null!");

		return setEX(Mono.just(SetCommand.set(key).value(value).expiring(expireTimeout))).next()
				.map(BooleanResponse::getOutput);
	}

	/**
	 * Set {@literal key value} pairs and {@link Expiration}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setex">Redis Documentation: SETEX</a>
	 */
	Flux<BooleanResponse<SetCommand>> setEX(Publisher<SetCommand> commands);

	/**
	 * Set {@literal key value} pair and {@link Expiration}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param expireTimeout must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/psetex">Redis Documentation: PSETEX</a>
	 */
	default Mono<Boolean> pSetEX(ByteBuffer key, ByteBuffer value, Expiration expireTimeout) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");
		Assert.notNull(key, "ExpireTimeout must not be null!");

		return pSetEX(Mono.just(SetCommand.set(key).value(value).expiring(expireTimeout))).next()
				.map(BooleanResponse::getOutput);
	}

	/**
	 * Set {@literal key value} pairs and {@link Expiration}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/psetex">Redis Documentation: PSETEX</a>
	 */
	Flux<BooleanResponse<SetCommand>> pSetEX(Publisher<SetCommand> commands);

	/**
	 * {@code MSET} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/mset">Redis Documentation: MSET</a>
	 */
	class MSetCommand implements Command {

		private Map<ByteBuffer, ByteBuffer> keyValuePairs;

		private MSetCommand(Map<ByteBuffer, ByteBuffer> keyValuePairs) {
			this.keyValuePairs = keyValuePairs;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.redis.connection.ReactiveRedisConnection.Command#getKey()
		 */
		@Override
		public ByteBuffer getKey() {
			return null;
		}

		/**
		 * Creates a new {@link MSetCommand} given a {@link Map} of key-value tuples.
		 *
		 * @param keyValuePairs must not be {@literal null}.
		 * @return a new {@link MSetCommand} for a {@link Map} of key-value tuples.
		 */
		public static MSetCommand mset(Map<ByteBuffer, ByteBuffer> keyValuePairs) {

			Assert.notNull(keyValuePairs, "Key-value pairs must not be null!");

			return new MSetCommand(keyValuePairs);
		}

		/**
		 * @return
		 */
		public Map<ByteBuffer, ByteBuffer> getKeyValuePairs() {
			return keyValuePairs;
		}
	}

	/**
	 * Set multiple keys to multiple values using key-value pairs provided in {@literal tuple}.
	 *
	 * @param keyValuePairs must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/mset">Redis Documentation: MSET</a>
	 */
	default Mono<Boolean> mSet(Map<ByteBuffer, ByteBuffer> keyValuePairs) {

		Assert.notNull(keyValuePairs, "Key-value pairs must not be null!");

		return mSet(Mono.just(MSetCommand.mset(keyValuePairs))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Set multiple keys to multiple values using key-value pairs provided in {@literal commands}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/mset">Redis Documentation: MSET</a>
	 */
	Flux<BooleanResponse<MSetCommand>> mSet(Publisher<MSetCommand> commands);

	/**
	 * Set multiple keys to multiple values using key-value pairs provided in {@literal keyValuePairs} only if the
	 * provided key does not exist.
	 *
	 * @param keyValuePairs must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/msetnx">Redis Documentation: MSETNX</a>
	 */
	default Mono<Boolean> mSetNX(Map<ByteBuffer, ByteBuffer> keyValuePairs) {

		Assert.notNull(keyValuePairs, "Key-value pairs must not be null!");

		return mSetNX(Mono.just(MSetCommand.mset(keyValuePairs))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Set multiple keys to multiple values using key-value pairs provided in {@literal tuples} only if the provided key
	 * does not exist.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/msetnx">Redis Documentation: MSETNX</a>
	 */
	Flux<BooleanResponse<MSetCommand>> mSetNX(Publisher<MSetCommand> source);

	/**
	 * {@code APPEND} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/append">Redis Documentation: APPEND</a>
	 */
	class AppendCommand extends KeyCommand {

		private ByteBuffer value;

		private AppendCommand(ByteBuffer key, ByteBuffer value) {

			super(key);
			this.value = value;
		}

		/**
		 * Creates a new {@link AppendCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link AppendCommand} for a {@literal key}.
		 */
		public static AppendCommand key(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new AppendCommand(key, null);
		}

		/**
		 * Applies the {@literal value} to append. Constructs a new command instance with all previously configured
		 * properties.
		 *
		 * @param value must not be {@literal null}.
		 * @return a new {@link AppendCommand} with {@literal value} applied.
		 */
		public AppendCommand append(ByteBuffer value) {

			Assert.notNull(value, "Value must not be null!");

			return new AppendCommand(getKey(), value);
		}

		/**
		 * @return
		 */
		public ByteBuffer getValue() {
			return value;
		}
	}

	/**
	 * Append a {@literal value} to {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/append">Redis Documentation: APPEND</a>
	 */
	default Mono<Long> append(ByteBuffer key, ByteBuffer value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return append(Mono.just(AppendCommand.key(key).append(value))).next().map(NumericResponse::getOutput);
	}

	/**
	 * Append a {@link AppendCommand#getValue()} to the {@link AppendCommand#getKey()}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/append">Redis Documentation: APPEND</a>
	 */
	Flux<NumericResponse<AppendCommand, Long>> append(Publisher<AppendCommand> commands);

	/**
	 * Get a substring of value of {@literal key} between {@literal begin} and {@literal end}.
	 *
	 * @param key must not be {@literal null}.
	 * @param begin
	 * @param end
	 * @return
	 * @see <a href="http://redis.io/commands/getrange">Redis Documentation: GETRANGE</a>
	 */
	default Mono<ByteBuffer> getRange(ByteBuffer key, long begin, long end) {

		Assert.notNull(key, "Key must not be null!");

		return getRange(Mono.just(RangeCommand.key(key).fromIndex(begin).toIndex(end))) //
				.next() //
				.map(ByteBufferResponse::getOutput);
	}

	/**
	 * Get a substring of value of {@literal key} between {@literal begin} and {@literal end}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/getrange">Redis Documentation: GETRANGE</a>
	 */
	Flux<ByteBufferResponse<RangeCommand>> getRange(Publisher<RangeCommand> commands);

	/**
	 * {@code SETRANGE} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
	 */
	class SetRangeCommand extends KeyCommand {

		private ByteBuffer value;
		private Long offset;

		private SetRangeCommand(ByteBuffer key, ByteBuffer value, Long offset) {

			super(key);
			this.value = value;
			this.offset = offset;
		}

		/**
		 * Creates a new {@link SetRangeCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link SetRangeCommand} for a {@literal key}.
		 */
		public static SetRangeCommand overwrite(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new SetRangeCommand(key, null, null);
		}

		/**
		 * Applies the {@literal value}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param value must not be {@literal null}.
		 * @return a new {@link SetCommand} with {@literal value} applied.
		 */
		public SetRangeCommand withValue(ByteBuffer value) {

			Assert.notNull(value, "Value must not be null!");

			return new SetRangeCommand(getKey(), value, offset);
		}

		/**
		 * Applies the {@literal index}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param index
		 * @return a new {@link SetRangeCommand} with {@literal key} applied.
		 */
		public SetRangeCommand atPosition(long index) {
			return new SetRangeCommand(getKey(), value, index);
		}

		/**
		 * @return
		 */
		public ByteBuffer getValue() {
			return value;
		}

		/**
		 * @return
		 */
		public Long getOffset() {
			return offset;
		}
	}

	/**
	 * Overwrite parts of {@literal key} starting at the specified {@literal offset} with given {@literal value}.
	 *
	 * @param key must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @param offset
	 * @return
	 * @see <a href="http://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
	 */
	default Mono<Long> setRange(ByteBuffer key, ByteBuffer value, long offset) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return setRange(Mono.just(SetRangeCommand.overwrite(key).withValue(value).atPosition(offset))).next()
				.map(NumericResponse::getOutput);
	}

	/**
	 * Overwrite parts of {@link SetRangeCommand#key} starting at the specified {@literal offset} with given
	 * {@link SetRangeCommand#value}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setrange">Redis Documentation: SETRANGE</a>
	 */
	Flux<NumericResponse<SetRangeCommand, Long>> setRange(Publisher<SetRangeCommand> commands);

	/**
	 * {@code GETBIT} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/getbit">Redis Documentation: GETBIT</a>
	 */
	class GetBitCommand extends KeyCommand {

		private Long offset;

		private GetBitCommand(ByteBuffer key, Long offset) {

			super(key);

			this.offset = offset;
		}

		/**
		 * Creates a new {@link GetBitCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link GetBitCommand} for a {@literal key}.
		 */
		public static GetBitCommand bit(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new GetBitCommand(key, null);
		}

		/**
		 * Applies the offset {@literal index}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param offset
		 * @return a new {@link GetBitCommand} with {@literal offset} applied.
		 */
		public GetBitCommand atOffset(long offset) {
			return new GetBitCommand(getKey(), offset);
		}

		/**
		 * @return
		 */
		public Long getOffset() {
			return offset;
		}
	}

	/**
	 * Get the bit value at {@literal offset} of value at {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @param offset
	 * @return
	 * @see <a href="http://redis.io/commands/getbit">Redis Documentation: GETBIT</a>
	 */
	default Mono<Boolean> getBit(ByteBuffer key, long offset) {

		Assert.notNull(key, "Key must not be null!");

		return getBit(Mono.just(GetBitCommand.bit(key).atOffset(offset))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Get the bit value at {@literal offset} of value at {@literal key}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/getbit">Redis Documentation: GETBIT</a>
	 */
	Flux<BooleanResponse<GetBitCommand>> getBit(Publisher<GetBitCommand> commands);

	/**
	 * {@code SETBIT} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/setbit">Redis Documentation: SETBIT</a>
	 */
	class SetBitCommand extends KeyCommand {

		private Long offset;
		private boolean value;

		private SetBitCommand(ByteBuffer key, Long offset, boolean value) {

			super(key);

			this.offset = offset;
			this.value = value;
		}

		/**
		 * Creates a new {@link SetBitCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link SetBitCommand} for a {@literal key}.
		 */
		public static SetBitCommand bit(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new SetBitCommand(key, null, false);
		}

		/**
		 * Applies the offset {@literal index}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param index
		 * @return a new {@link SetBitCommand} with {@literal offset} applied.
		 */
		public SetBitCommand atOffset(long index) {
			return new SetBitCommand(getKey(), index, value);
		}

		/**
		 * Applies the {@literal bit}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param bit
		 * @return a new {@link SetBitCommand} with {@literal offset} applied.
		 */
		public SetBitCommand to(boolean bit) {
			return new SetBitCommand(getKey(), offset, bit);
		}

		/**
		 * @return
		 */
		public Long getOffset() {
			return offset;
		}

		/**
		 * @return
		 */
		public boolean getValue() {
			return value;
		}
	}

	/**
	 * Sets the bit at {@literal offset} in value stored at {@literal key} and return the original value.
	 *
	 * @param key must not be {@literal null}.
	 * @param offset
	 * @param value
	 * @return
	 */
	default Mono<Boolean> setBit(ByteBuffer key, long offset, boolean value) {

		Assert.notNull(key, "Key must not be null!");

		return setBit(Mono.just(SetBitCommand.bit(key).atOffset(offset).to(value))).next().map(BooleanResponse::getOutput);
	}

	/**
	 * Sets the bit at {@literal offset} in value stored at {@literal key} and return the original value.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/setbit">Redis Documentation: SETBIT</a>
	 */
	Flux<BooleanResponse<SetBitCommand>> setBit(Publisher<SetBitCommand> commands);

	/**
	 * {@code BITCOUNT} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/bitcount">Redis Documentation: BITCOUNT</a>
	 */
	class BitCountCommand extends KeyCommand {

		private Range<Long> range;

		private BitCountCommand(ByteBuffer key, Range<Long> range) {

			super(key);

			this.range = range;
		}

		/**
		 * Creates a new {@link BitCountCommand} given a {@literal key}.
		 *
		 * @param key must not be {@literal null}.
		 * @return a new {@link BitCountCommand} for a {@literal key}.
		 */
		public static BitCountCommand bitCount(ByteBuffer key) {

			Assert.notNull(key, "Key must not be null!");

			return new BitCountCommand(key, null);
		}

		/**
		 * Applies the {@link Range}. Constructs a new command instance with all previously configured properties.
		 *
		 * @param range must not be {@literal null}.
		 * @return a new {@link BitCountCommand} with {@link Range} applied.
		 */
		public BitCountCommand within(Range<Long> range) {

			Assert.notNull(range, "Range must not be null!");

			return new BitCountCommand(getKey(), range);
		}

		/**
		 * @return
		 */
		public Range<Long> getRange() {
			return range;
		}
	}

	/**
	 * Count the number of set bits (population counting) in value stored at {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/bitcount">Redis Documentation: BITCOUNT</a>
	 */
	default Mono<Long> bitCount(ByteBuffer key) {

		Assert.notNull(key, "Key must not be null!");

		return bitCount(Mono.just(BitCountCommand.bitCount(key))).next().map(NumericResponse::getOutput);
	}

	/**
	 * Count the number of set bits (population counting) of value stored at {@literal key} between {@literal begin} and
	 * {@literal end}.
	 *
	 * @param key must not be {@literal null}.
	 * @param begin
	 * @param end
	 * @return
	 * @see <a href="http://redis.io/commands/bitcount">Redis Documentation: BITCOUNT</a>
	 */
	default Mono<Long> bitCount(ByteBuffer key, long begin, long end) {

		Assert.notNull(key, "Key must not be null!");

		return bitCount(Mono.just(BitCountCommand.bitCount(key).within(new Range<>(begin, end)))).next()
				.map(NumericResponse::getOutput);
	}

	/**
	 * Count the number of set bits (population counting) of value stored at {@literal key} between {@literal begin} and
	 * {@literal end}.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/bitcount">Redis Documentation: BITCOUNT</a>
	 */
	Flux<NumericResponse<BitCountCommand, Long>> bitCount(Publisher<BitCountCommand> commands);

	/**
	 * {@code BITOP} command parameters.
	 *
	 * @author Christoph Strobl
	 * @see <a href="http://redis.io/commands/bitop">Redis Documentation: BITOP</a>
	 */
	class BitOpCommand {

		private List<ByteBuffer> keys;
		private BitOperation bitOp;
		private ByteBuffer destinationKey;

		private BitOpCommand(List<ByteBuffer> keys, BitOperation bitOp, ByteBuffer destinationKey) {

			this.keys = keys;
			this.bitOp = bitOp;
			this.destinationKey = destinationKey;
		}

		/**
		 * Creates a new {@link BitOpCommand} given a {@link BitOperation}.
		 *
		 * @param bitOp must not be {@literal null}.
		 * @return a new {@link BitCountCommand} for a {@link BitOperation}.
		 */
		public static BitOpCommand perform(BitOperation bitOp) {

			Assert.notNull(bitOp, "BitOperation must not be null!");

			return new BitOpCommand(null, bitOp, null);
		}

		/**
		 * Applies the operation to {@literal keys}. Constructs a new command instance with all previously configured
		 * properties.
		 *
		 * @param keys must not be {@literal null}.
		 * @return a new {@link BitOpCommand} with {@link Range} applied.
		 */
		public BitOpCommand onKeys(Collection<ByteBuffer> keys) {

			Assert.notNull(keys, "Keys must not be null!");

			return new BitOpCommand(new ArrayList<>(keys), bitOp, destinationKey);
		}

		/**
		 * Applies the {@literal key} to store the result at. Constructs a new command instance with all previously
		 * configured properties.
		 *
		 * @param destinationKey must not be {@literal null}.
		 * @return a new {@link BitOpCommand} with {@link Range} applied.
		 */
		public BitOpCommand andSaveAs(ByteBuffer destinationKey) {

			Assert.notNull(destinationKey, "Destination key must not be null!");

			return new BitOpCommand(keys, bitOp, destinationKey);
		}

		/**
		 * @return
		 */
		public BitOperation getBitOp() {
			return bitOp;
		}

		/**
		 * @return
		 */
		public List<ByteBuffer> getKeys() {
			return keys;
		}

		/**
		 * @return
		 */
		public ByteBuffer getDestinationKey() {
			return destinationKey;
		}
	}

	/**
	 * Perform bitwise operations between strings.
	 *
	 * @param keys must not be {@literal null}.
	 * @param bitOp must not be {@literal null}.
	 * @param destination must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/bitop">Redis Documentation: BITOP</a>
	 */
	default Mono<Long> bitOp(Collection<ByteBuffer> keys, BitOperation bitOp, ByteBuffer destination) {

		Assert.notNull(keys, "Keys must not be null!");
		Assert.notNull(bitOp, "BitOperation must not be null!");
		Assert.notNull(destination, "Destination must not be null!");

		return bitOp(Mono.just(BitOpCommand.perform(bitOp).onKeys(keys).andSaveAs(destination))) //
				.next() //
				.map(NumericResponse::getOutput);
	}

	/**
	 * Perform bitwise operations between strings.
	 *
	 * @param commands must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/bitop">Redis Documentation: BITOP</a>
	 */
	Flux<NumericResponse<BitOpCommand, Long>> bitOp(Publisher<BitOpCommand> commands);

	/**
	 * Get the length of the value stored at {@literal key}.
	 *
	 * @param key must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/strlen">Redis Documentation: STRLEN</a>
	 */
	default Mono<Long> strLen(ByteBuffer key) {

		Assert.notNull(key, "Key must not be null!");

		return strLen(Mono.just(new KeyCommand(key))).next().map(NumericResponse::getOutput);
	}

	/**
	 * Get the length of the value stored at {@literal key}.
	 *
	 * @param keys must not be {@literal null}.
	 * @return
	 * @see <a href="http://redis.io/commands/strlen">Redis Documentation: STRLEN</a>
	 */
	Flux<NumericResponse<KeyCommand, Long>> strLen(Publisher<KeyCommand> keys);
}
