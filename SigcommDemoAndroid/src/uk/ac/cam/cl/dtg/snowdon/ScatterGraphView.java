/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.snowdon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * Inherits from GraphView and draws a graph with a simple marker at data
 * points.
 */
public class ScatterGraphView extends GraphView {
	/**
	 * Creates a new ScatterGraphView and sets layout parameters from XML via
	 * GraphView.
	 * 
	 * @param context
	 * @param attributes
	 */
	public ScatterGraphView(Context context, AttributeSet attributes) {
		super(context, attributes);
	}
	
	@Override
    protected void drawPlot(Canvas canvas) {
    	for (int set = 0; set < mData.length; set++) {
    		try {
				int setColour = mDataSetColours[set];
				mAscendingPaint.setColor(setColour);
				mDescendingPaint.setColor(setColour);
			} catch (IndexOutOfBoundsException e) {
				// Just carry on using the last colour
			}
    		
    		for (int i = 0; i < mData[set][0].length - 1; i++) {
    			float[] coords = calcCoordinates(set, i);
    			if (Float.isNaN(mData[set][1][i + 1])) {
    				// Don't draw anything for NaNs, as it doesn't make sense to
    			} else if (Float.isNaN(mData[set][1][i])
    					&& Float.isNaN(mData[set][1][i + 1]) == false) {
    				// More NaNs
    			} else if (Float.isNaN(coords[1]) || Float.isNaN(coords[3])) {
    				// Yet more NaNs
    			} else if (coords[3] < coords[1]) {
    				// If the data is increasing, draw in the ascending style...
    				drawAscendingSection(canvas, coords);
    			} else {
    				// ...otherwise, draw in the descending style
    				drawDescendingSection(canvas, coords);
    			}
    		}
    		
    		float[] coords = calcCoordinates(set, mData[set][0].length - 2);
    		canvas.drawLine(coords[2] - mAscendingPaint.getStrokeWidth() * 3, coords[3], coords[2] + mAscendingPaint.getStrokeWidth() * 3, coords[3], mAscendingPaint);
    		canvas.drawLine(coords[2], coords[3] - mAscendingPaint.getStrokeWidth() * 3, coords[2], coords[3] + mAscendingPaint.getStrokeWidth() * 3, mAscendingPaint);
    	}
    }

	@Override
	protected void drawAscendingSection(Canvas canvas, float[] coords) {
		canvas.drawLine(coords[0] - mAscendingPaint.getStrokeWidth() * 3, coords[1], coords[0] + mAscendingPaint.getStrokeWidth() * 3, coords[1], mAscendingPaint);
		canvas.drawLine(coords[0], coords[1] - mAscendingPaint.getStrokeWidth() * 3, coords[0], coords[1] + mAscendingPaint.getStrokeWidth() * 3, mAscendingPaint);
	}

	@Override
	protected void drawDescendingSection(Canvas canvas, float[] coords) {
		canvas.drawLine(coords[0] - mDescendingPaint.getStrokeWidth() * 3, coords[1], coords[0] + mDescendingPaint.getStrokeWidth() * 3, coords[1], mDescendingPaint);
		canvas.drawLine(coords[0], coords[1] - mDescendingPaint.getStrokeWidth() * 3, coords[0], coords[1] + mDescendingPaint.getStrokeWidth() * 3, mDescendingPaint);
	}
}