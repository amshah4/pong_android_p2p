/*****************************************************************************
 * Ankoor Shah
 * p2p pong, MainActivity Class
 * Class for application management, handling events such as launch, closed,
 * minimized/paused, resumed, etc.
 *
 * COMMENTS ARE LEFT UNTOUCHED.
 ****************************************************************************/
package com.nick.aponggame;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity
{
	//constants
	public static final String GAME_MODE = "com.example.myfirstapp.GAME_MODE";//key for game mode name sent by intent
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void button0Clicked(View current)
    {
    	Intent intent=new Intent(this, GameModeActivity.class);
    	intent.putExtra(GAME_MODE, "2p0");//type of game this button represented
    	startActivity(intent);
        return;
    }
    
    public void button1Clicked(View current)
    {
    	Intent intent=new Intent(this, GameModeActivity.class);
    	intent.putExtra(GAME_MODE, "2p1");//type of game this button represented
    	startActivity(intent);
        return;
    }
    
    /*
	 * IDEAS:
	 * 		Make two different draw functions: draw paddle and draw ball, doesn't redraw
	 * 		paddle unless touch event happens
	 * 		
	 * 		netcode is a class extends thread, use mutex for handling a queue of sent data
	 * 		
	 */
}
