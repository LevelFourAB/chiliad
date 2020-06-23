package se.l4.chiliad.transport.netty.handlers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import se.l4.chiliad.engine.transport.handshake.Auth;
import se.l4.chiliad.engine.transport.handshake.AuthData;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.Hello;
import se.l4.chiliad.engine.transport.handshake.Ok;
import se.l4.chiliad.engine.transport.handshake.Reject;
import se.l4.chiliad.engine.transport.handshake.Select;

/**
 * Codec that encodes and decodes the messages used during the initial
 * protocol handshake.
 */
public class HandshakeCodec
	extends ByteToMessageCodec<Object>
{
	private final int TAG_OK = 0;
	private final int TAG_REJECT = 1;

	private final int TAG_HELLO = 2;
	private final int TAG_SELECT = 3;

	private final int TAG_AUTH = 4;
	private final int TAG_AUTHDATA = 5;

	private final int TAG_BEGIN = 6;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
		throws Exception
	{
		if(in.readableBytes() < 1)
		{
			return;
		}

		in.markReaderIndex();
		Object decoded = decode(in);

		if(decoded == null)
		{
			in.resetReaderIndex();
		}
		else
		{
			out.add(decoded);
		}
	}

	private Object decode(ByteBuf in)
	{
		// Read the first byte
		switch(in.readByte())
		{
			case TAG_OK:
				return Ok.INSTANCE;
			case TAG_REJECT:
				return Reject.INSTANCE;
			case TAG_BEGIN:
				return Begin.INSTANCE;
			case TAG_HELLO:
			{
				String[] caps = readStringArray(in);
				if(caps == null) return null;

				return new Hello(caps);
			}
			case TAG_SELECT:
			{
				String[] caps = readStringArray(in);
				if(caps == null) return null;

				return new Select(caps);
			}
			case TAG_AUTH:
			{
				String method = readString(in);
				if(method == null) return null;

				ByteBuf data = readBytes(in);
				if(data == null) return null;

				return new Auth(method, data);
			}
			case TAG_AUTHDATA:
			{
				ByteBuf data = readBytes(in);
				if(data == null) return null;

				return new AuthData(data);
			}
		}

		throw new CorruptedFrameException("Unknown handshake");
	}

	private String[] readStringArray(ByteBuf in)
	{
		if(! in.isReadable())
		{
			return null;
		}

		int length = in.readByte();
		String[] result = new String[length];
		for(int i=0; i<length; i++)
		{
			String value = readString(in);
			if(value == null) return null;

			result[i] = value;
		}

		return result;
	}

	private String readString(ByteBuf in)
	{
		if(! in.isReadable())
		{
			// Can't read the length
			return null;
		}

		int valueLength = in.readByte();
		if(! in.isReadable(valueLength))
		{
			return null;
		}

		return in.readCharSequence(valueLength, StandardCharsets.UTF_8).toString();
	}

	private ByteBuf readBytes(ByteBuf in)
	{
		if(! in.isReadable(4))
		{
			return null;
		}

		int length = in.readInt();
		if(! in.isReadable(length))
		{
			return null;
		}

		return in.readBytes(length);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
		throws Exception
	{
		if(msg instanceof Ok)
		{
			out.writeByte(TAG_OK);
		}
		else if(msg instanceof Reject)
		{
			out.writeByte(TAG_REJECT);
		}
		else if(msg instanceof Begin)
		{
			out.writeByte(TAG_BEGIN);
		}
		else if(msg instanceof Hello)
		{
			out.writeByte(TAG_HELLO);

			String[] caps = ((Hello) msg).getCapabilities();
			encodeStringArray(out, caps);
		}
		else if(msg instanceof Select)
		{
			out.writeByte(TAG_SELECT);

			String[] caps = ((Select) msg).getCapabilities();
			encodeStringArray(out, caps);
		}
		else if(msg instanceof Auth)
		{
			out.writeByte(TAG_AUTH);

			encodeString(out, ((Auth) msg).getMethod());

			ByteBuf data = ((Auth) msg).getData();
			out.writeInt(data.readableBytes());
			out.writeBytes(data);

			data.release();
		}
		else if(msg instanceof AuthData)
		{
			out.writeByte(TAG_AUTHDATA);

			ByteBuf data = ((AuthData) msg).getData();
			out.writeInt(data.readableBytes());
			out.writeBytes(data);

			data.release();
		}
	}

	private void encodeStringArray(ByteBuf out, String[] array)
	{
		out.writeByte(array.length);
		for(String value : array)
		{
			encodeString(out, value);
		}
	}

	private void encodeString(ByteBuf out, String value)
	{
		byte[] binary = value.getBytes(StandardCharsets.UTF_8);
		out.writeByte(binary.length);
		out.writeBytes(binary);
	}
}
