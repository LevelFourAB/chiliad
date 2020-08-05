package se.l4.chiliad.engine.internal.protocol;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import se.l4.chiliad.ServiceException;
import se.l4.exobytes.Serializer;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingOutput;

/**
 * Helper to encode payloads.
 */
public class InvocationPayloadEncoding
{
	private InvocationPayloadEncoding()
	{
	}

	/**
	 * Create the initial metadata for the given service and method.
	 *
	 * @param service
	 *   service
	 * @param method
	 *   method on the service
	 * @return
	 *   initial metadata buffer
	 */
	public static ByteBuf createMetadata(
		String service,
		String method
	)
	{
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

		// Version tag
		buf.writeByte(1);

		encodeString(service, buf);
		encodeString(method, buf);

		return buf;
	}

	public static void encodeString(String value, ByteBuf buf)
	{
		int length = ByteBufUtil.utf8Bytes(value);
		buf.writeShort(length);
		ByteBufUtil.reserveAndWriteUtf8(buf, value, length);
	}

	/**
	 * Encode an object using the given codec and return a new buffer.
	 *
	 * @param codec
	 * @param data
	 * @return
	 */
	public static ByteBuf encodeObject(Serializer<?> codec, Object data)
	{
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
		try(StreamingOutput out = createOutput(buf))
		{
			encodeObject(codec, data, out);
		}
		catch(IOException e)
		{
			buf.release();

			throw new ServiceException("Unable to encode object; " + e.getMessage(), e);
		}
		catch(RuntimeException | Error t)
		{
			buf.release();

			throw t;
		}

		return buf;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void encodeObject(Serializer codec, Object data, StreamingOutput out)
	{
		try
		{
			((Serializer) codec).write(data, out);
		}
		catch(IOException e)
		{
			throw new ServiceException("Unable to encode object; " + e.getMessage(), e);
		}
	}

	/**
	 * Encode an object using the given codec and return a new buffer.
	 *
	 * @param codec
	 * @param data
	 * @return
	 */
	public static ByteBuf encodeArguments(
		Serializer<?>[] codecs,
		Object[] arguments
	)
	{
		ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
		try(StreamingOutput out = createOutput(buf))
		{
			encodeArguments(codecs, arguments, out);
		}
		catch(IOException e)
		{
			buf.release();

			throw new ServiceException("Unable to encode object; " + e.getMessage(), e);
		}
		catch(RuntimeException | Error t)
		{
			buf.release();

			throw t;
		}

		return buf;
	}

	public static void encodeArguments(
		Serializer<?>[] codecs,
		Object[] arguments,
		StreamingOutput out
	)
		throws IOException
	{
		out.writeListStart(codecs.length);
		for(int i=0, n=arguments.length; i<n; i++)
		{
			encodeObject(codecs[i], arguments[i], out);
		}
		out.writeListEnd();
	}

	public static StreamingOutput createOutput(ByteBuf buf)
		throws IOException
	{
		return StreamingFormat.CBOR.createOutput(new ByteBufOutputStream(buf));
	}
}
