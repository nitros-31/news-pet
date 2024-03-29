package edu.iastate.coms.cs472.newspet.trainer;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.iastate.coms.cs472.newspet.utils.MessageQueueThread;
import edu.iastate.coms.cs472.newspet.utils.dal.ConnectionConfig;

/**
 * @author Michael Fulker
 */
public class TrainerService
{
	private MessageQueueThread queue;
	
	private ThreadPoolExecutor threadPool;
	
	public TrainerService(int port, int timeout)
	{
		if(timeout == -1) queue = new MessageQueueThread(port);
		else queue = new MessageQueueThread(port, timeout);
		
		//TODO: fine-tune / have configurable params
		threadPool = new ThreadPoolExecutor(32, 32, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public static void main(String[] args)
	{
		if(args.length != 5) throw new IllegalArgumentException("Requires parameters: PORT TIMEOUT DBPATH LOGIN PASSWORD");
		
		int port = Integer.parseInt(args[0]);
		
		int timeout = Integer.parseInt(args[1]);
		
		ConnectionConfig.setupParams(args[2], args[3], args[4]);
		
		(new TrainerService(port, timeout)).run();
	}
	
	private void run()
	{	
		queue.start();
		
		while(true)
		{
			String incomingString = blockingPeekInQueue();
			int currentClassifierID = (new Message(incomingString)).getUserId();
			
			//get job of contiguous items for specific classifier
			TrainerThreadJob job = new TrainerThreadJob(currentClassifierID);
			Message currentMessage;
			while(!queue.getMessageQueue().isEmpty() && (currentMessage = new Message(blockingPeekInQueue())).getUserId() == currentClassifierID)
			{
				job.add(currentMessage);
				queue.getMessageQueue().remove();
			}
			
			//give to threadpool
			threadPool.execute(job);
		}
	}
	
	private String blockingPeekInQueue()
	{
		//block until something in queue
		try
		{
			return queue.getMessageQueue().blockingPeek();
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException("InterruptedException during queue block", e);
		}
	}
}
