package cl.signpost.narseo.com;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class IntroView extends Activity  {

	private static ImageButton button = null;
	private static EditText editText = null;
	private static TextView errorTextView = null;
	private static final String TAG = "INTRO";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		editText = (EditText) findViewById(R.id.editText1);
		errorTextView = (TextView) findViewById (R.id.textView2);
		addListenerOnButton();
	}
 
	public void addListenerOnButton() { 
		final Context context = this;
 
		button = (ImageButton) findViewById(R.id.imageButton2);
 
		button.setOnClickListener(new OnClickListener() {
 
			@Override
			public void onClick(View arg0) { 
				Log.i(TAG, "Button Pressed");
				String name = editText.getText().toString();
				if (name.equals(".signpo.st") || name.contains(" ") || name.contains(";") || name.contains(":") || name.endsWith(".signpo.st")==false){

					Log.i(TAG, "INVALID NAME");
					errorTextView.setText("ERROR: Invalid DNS Name!");
				}
				else{
					SigcommDemoAndroidService.setName(name);
					errorTextView.setText("");
					Intent intent = new Intent(context, SigcommDemoAndroidActivity.class);
                    startActivity(intent);
				}
			       
 
			}
 
		});
 
	}
 
	
	
	

}
