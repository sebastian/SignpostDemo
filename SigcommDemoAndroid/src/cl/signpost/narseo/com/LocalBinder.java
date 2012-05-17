package cl.signpost.narseo.com;

import java.lang.ref.WeakReference;

import android.os.Binder;


public class LocalBinder<S> extends Binder{
	private String TAG = "LocalBinder";
	private WeakReference<S> mService;
	
	public LocalBinder (S service){
		mService = new WeakReference<S>(service);
	}
	
	public S getService(){
		return mService.get();
	}
}