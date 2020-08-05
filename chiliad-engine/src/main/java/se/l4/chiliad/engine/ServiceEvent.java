package se.l4.chiliad.engine;

import java.util.Objects;
import java.util.function.Predicate;

public class ServiceEvent
{
	public enum Type
	{
		/**
		 * Service has become available.
		 */
		AVAILABLE,

		/**
		 * Service is no longer available.
		 */
		UNAVAILABLE
	}

	private final Type type;
	private final String name;

	public ServiceEvent(Type type, String name)
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
		ServiceEvent other = (ServiceEvent) obj;
		return Objects.equals(name, other.name) && type == other.type;
	}

	@Override
	public String toString()
	{
		return "ServiceEvent{type=" + type + ", name=" + name + "}";
	}

	public static Predicate<? super ServiceEvent> isAvailable(String name)
	{
		return event -> event.getType() == Type.AVAILABLE && name.equals(event.getName());
	}

	public static Predicate<? super ServiceEvent> isUnavailable(String name)
	{
		return event -> event.getType() == Type.UNAVAILABLE && name.equals(event.getName());
	}
}
