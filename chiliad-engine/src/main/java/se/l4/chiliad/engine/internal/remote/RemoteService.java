package se.l4.chiliad.engine.internal.remote;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.rsocket.RSocket;
import se.l4.chiliad.ServiceException;

/**
 * Information about a service that is reachable via one or more {@link RSocket}
 * instances.
 */
public class RemoteService
{
	private static final RSocket[] EMPTY = new RSocket[0];

	private final ReadWriteLock lock;
	private RSocket[] sockets;

	public RemoteService()
	{
		this.sockets = EMPTY;
		lock = new ReentrantReadWriteLock();
	}

	public boolean addSocket(RSocket socket)
	{
		lock.writeLock().lock();
		try
		{
			RSocket[] sockets = Arrays.copyOf(this.sockets, this.sockets.length + 1);
			sockets[sockets.length - 1] = socket;
			this.sockets = sockets;

			return sockets.length == 1;
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public boolean removeSocket(RSocket socket)
	{
		lock.writeLock().lock();
		try
		{
			Object[] sockets = this.sockets;
			int index = -1;
			for(int i=0, n=sockets.length; i<n; i++)
			{
				if(sockets[i] == socket)
				{
					index = i;
					break;
				}
			}

			if(index == -1)
			{
				// Nothing to do, no such listener
				return false;
			}

			int length = sockets.length;
			RSocket[] result = new RSocket[length - 1];
			System.arraycopy(sockets, 0, result, 0, index);

			if(index < length - 1)
			{
				System.arraycopy(sockets, index + 1, result, index, length - index - 1);
			}

			this.sockets = result;

			return result.length == 0;
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public RSocket pickRandom()
	{
		RSocket[] sockets = this.sockets;
		if(sockets.length == 0)
		{
			throw new ServiceException("Service is not currently available");
		}

		return sockets[ThreadLocalRandom.current().nextInt(sockets.length)];
	}
}
