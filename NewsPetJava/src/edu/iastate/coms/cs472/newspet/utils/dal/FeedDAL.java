package edu.iastate.coms.cs472.newspet.utils.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.iastate.coms.cs472.newspet.utils.Feed;

/**
 * Data access layer for Classifier objects.
 * 
 * @author Michael Fulker
 */
public class FeedDAL
{
	static final String ID_COLUMN = "id";
	
	static final String URL_COLUMN = "url";
	
	static final String USERID_COLUMN = "subscriber_id";
	
	static final String LASTCRAWLED_COLUMN = "last_crawled";
	
	static final String FEED_TABLE = "feed_feed";
	
	/**
	 * Gets feed objects with "lastCrawled" timestamps older than the given
	 * cutoff time.
	 * 
	 * @param cutoff
	 *        Cutoff time in UTC form.
	 * @return
	 */
	public static List<Feed> getFeedsOlderThan(java.util.Date cutoff)
	{
		Connection conn = ConnectionConfig.createConnection();
		String query = "SELECT " + ID_COLUMN + ", " + URL_COLUMN + ", " + USERID_COLUMN + ", " + LASTCRAWLED_COLUMN + " FROM " + FEED_TABLE
				+ " WHERE " + LASTCRAWLED_COLUMN + " < datetime(?) ORDER BY " + LASTCRAWLED_COLUMN + " ASC;";
		
		PreparedStatement getFeeds = null;
		ResultSet feedResults = null;
		try
		{
			getFeeds = conn.prepareStatement(query);
			//TODO: Don't rely on a string conversion 
			getFeeds.setString(1, dateToString(cutoff));
			feedResults = getFeeds.executeQuery();
			
			List<Feed> toReturn = new ArrayList<Feed>();
			while(feedResults.next())
			{
				int feedID = feedResults.getInt(ID_COLUMN);
				String url = feedResults.getString(URL_COLUMN);
				int userID = feedResults.getInt(USERID_COLUMN);
				java.util.Date lastCrawled = feedResults.getDate(LASTCRAWLED_COLUMN);
				
				Feed toAdd = new Feed(feedID, url, userID, lastCrawled);
				toReturn.add(toAdd);
			}
			
			return toReturn;
		}
		catch(SQLException e)
		{
			throw new RuntimeException("Could not retrieve Feed records", e);
		}
		finally
		{
			if(feedResults != null) try
			{
				feedResults.close();
			}
			catch(SQLException e)
			{
				System.err.println("SQLException while trying to close a ResultSet!");
				System.err.println(e.getMessage());
			}
			try
			{
				if(getFeeds != null) getFeeds.close();
			}
			catch(SQLException e)
			{
				System.err.println("SQLException while trying to close a PreparedStatement!");
				System.err.println(e.getMessage());
			}
			try
			{
				if(conn != null)
				{
					if(!conn.getAutoCommit()) conn.commit();
					conn.close();
				}
			}
			catch(SQLException e)
			{
				System.err.println("SQLException while trying to close a Connection!");
				System.err.println(e.getMessage());
			}
		}
	}
	
	public static void updateCrawlTime(java.util.Date crawlTime, int feedID)
	{
		Connection conn = null;
		PreparedStatement update=null;
		
		try{
			conn = ConnectionConfig.createConnection();
			update = conn.prepareStatement(String.format("UPDATE %s SET %s=datetime(?) WHERE %s=?;", FEED_TABLE, LASTCRAWLED_COLUMN, ID_COLUMN));
			update.setString(1, dateToString(crawlTime));
			update.setInt(2,feedID);
			
			update.executeUpdate();
		}catch(SQLException e)
		{
			throw new RuntimeException("Could not update crawl time", e);
		}
		finally
		{
			try
			{
				if(update!= null) update.close();
			}
			catch(SQLException e)
			{
				System.err.println("SQLException while trying to close a PreparedStatement!");
				System.err.println(e.getMessage());
			}
			try
			{
				if(conn != null)
				{
					if(!conn.getAutoCommit()) conn.commit();
					conn.close();
				}
			}
			catch(SQLException e)
			{
				System.err.println("SQLException while trying to close a Connection!");
				System.err.println(e.getMessage());
			}
		}
	}
	
	private static String dateToString(Date cutoff)
	{
		return String.format("%tFT%tT", cutoff, cutoff);
	}
}
