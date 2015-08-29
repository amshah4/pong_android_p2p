/*****************************************************************************
 * Ankoor Shah
 * p2p pong, Paddle Class
 * Represents paddle object used to strike ball back.
 ****************************************************************************/
package com.nick.aponggame;

public class Paddle
{
	private int xPos;
	private int yPos;
	private int length;
	private int height;
	
	public Paddle(int x, int y, int len, int h)
	{
		xPos=x;
		yPos=y;
		length=len;
		height=h;
	}


    //GETTERS/SETTERS
	public int getX()
	{
		return xPos;
	}
	public int getY()
	{
		return yPos;
	}
	public int getLength()
	{
		return length;
	}
	public int getHeight()
	{
		return height;
	}
	public void setX(int x)
	{
		xPos=x;
		return;
	}
	public void setY(int y)
	{
		yPos=y;
		return;
	}
}
