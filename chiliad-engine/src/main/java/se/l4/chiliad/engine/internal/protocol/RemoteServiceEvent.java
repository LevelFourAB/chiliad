package se.l4.chiliad.engine.internal.protocol;

import java.io.IOException;
import java.util.Objects;

import se.l4.exobytes.SerializationException;
import se.l4.exobytes.Serializer;
import se.l4.exobytes.streaming.StreamingInput;
import se.l4.exobytes.streaming.StreamingOutput;
import se.l4.exobytes.streaming.Token;

/**
 * Event as received from remote peers.
 */
public class RemoteServiceEvent
{
	public enum Type
	{
		AVAILABLE,
		UNAVAILABLE;

		public int toRemoteCode()
		{
			return ordinal();
		}

		public static Type fromRemoteCode(int code)
		{
			switch(code)
			{
				case 0:
					return AVAILABLE;
				case 1:
					return UNAVAILABLE;
				default:
					throw new SerializationException("Unknown type of event " + code);
			}
		}
	}

	private final Type type;
	private final String name;

	private RemoteServiceEvent(Type type, String name)
	{
		this.type = type;
		this.name = name;
	}

	public Type getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, type);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if(obj == null) return false;
		if(getClass() != obj.getClass()) return false;
		RemoteServiceEvent other = (RemoteServiceEvent) obj;
		return Objects.equals(name, other.name) && type == other.type;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{type=" + type + ", name=" + name + "}";
	}

	public static RemoteServiceEvent available(String name)
	{
		return new RemoteServiceEvent(Type.AVAILABLE, name);
	}

	public static RemoteServiceEvent unavailable(String name)
	{
		return new RemoteServiceEvent(Type.UNAVAILABLE, name);
	}

	public static class SerializerImpl
		implements Serializer<RemoteServiceEvent>
	{
		@Override
		public RemoteServiceEvent read(StreamingInput in)
			throws IOException
		{
			in.next(Token.OBJECT_START);

			Type type = null;
			String name = null;
			do
			{
				in.next(Token.KEY);
				String key = in.readString();
				switch(key)
				{
					case "type":
						in.next(Token.VALUE);
						type = Type.fromRemoteCode(in.readInt());
						break;
					case "name":
						in.next(Token.VALUE);
						name = in.readString();
						break;
					default:
						in.skipNext();
				}

				if(type != null && name != null)
				{
					return new RemoteServiceEvent(type, name);
				}
			}
			while(in.peek() != Token.OBJECT_END);

			in.next(Token.OBJECT_END);
			return null;
		}

		@Override
		public void write(RemoteServiceEvent object, StreamingOutput out)
			throws IOException
		{
			out.writeObjectStart();
			out.writeString("type");
			out.writeInt(object.type.toRemoteCode());
			out.writeString("name");
			out.writeString(object.name);
			out.writeObjectEnd();
		}
	}
}
