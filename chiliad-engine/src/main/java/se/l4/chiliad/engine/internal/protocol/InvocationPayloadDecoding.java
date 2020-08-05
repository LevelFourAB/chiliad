package se.l4.chiliad.engine.internal.protocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import se.l4.chiliad.ServiceException;
import se.l4.exobytes.Serializer;
import se.l4.exobytes.streaming.StreamingFormat;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.Token;

/**
 * Helpers related to decoding of of payloads.
 */
public class InvocationPayloadDecoding
{
	public static String getService(ByteBuf metadata)
	{
		checkVersion(metadata);

		return readString(metadata, 1);
	}

	public static String getMethod(ByteBuf metadata)
	{
		checkVersion(metadata);

		int offset = 1 + Short.BYTES + metadata.getShort(1);
		return readString(metadata, offset);
	}

	private static void checkVersion(ByteBuf metadata)
	{
		if(metadata.getByte(0) != 1)
		{
			throw new IllegalArgumentException("Unknown version of metadata");
		}
	}

	public static String readString(ByteBuf buf, int offset)
	{
		int length = buf.getShort(offset);
		return buf.toString(offset + Short.BYTES, length, StandardCharsets.UTF_8);
	}

	public static <T> T readObject(Serializer<T> codec, ByteBuf data)
	{
		try(StreamingInput in = createInput(data))
		{
			return codec.read(in);
		}
		catch(IOException e)
		{
			throw new ServiceException("Unable to decode object; " + e.getMessage(), e);
		}
	}

	public static <T> T readObject(Serializer<T> codec, StreamingInput in)
	{
		try
		{
			return codec.read(in);
		}
		catch(IOException e)
		{
			throw new ServiceException("Unable to decode object; " + e.getMessage(), e);
		}
	}

	public static Object[] getArguments(Serializer<?>[] codecs, ByteBuf buf)
	{
		try(StreamingInput in = createInput(buf))
		{
			return getArguments(codecs, in);
		}
		catch(IOException e)
		{
			throw new ServiceException("Unable to decode; " + e.getMessage(), e);
		}
	}

	public static Object[] getArguments(Serializer<?>[] codecs, StreamingInput in)
		throws IOException
	{
		in.next(Token.LIST_START);
		if(in.getLength().isPresent() && in.getLength().getAsInt() != codecs.length)
		{
			throw new IOException("Got an unexpected number of arguments");
		}

		Object[] result = new Object[codecs.length];
		for(int i=0, n=codecs.length; i<n; i++)
		{
			result[i] = readObject(codecs[i], in);
		}

		in.next(Token.LIST_END);
		return result;
	}

	public static StreamingInput createInput(ByteBuf buf)
		throws IOException
	{
		return StreamingFormat.CBOR.createInput(new ByteBufInputStream(buf));
	}
}
