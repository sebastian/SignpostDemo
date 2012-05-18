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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * Inherits from GraphView and draws data as a histogram. Inputted data is assumed to already be in
 * frequency density format. Layout parameters are read in from XML.
 */
public class HistogramGraphView extends GraphView {
	protected Paint mLinePaint = new Paint();
	protected int[] bitmapColours;
	protected Bitmap mBitmap;
	protected Shader bandShader;
	protected Shader gradientShader;
	protected Shader compositeShader;
	protected Path mLinePath = new Path();
	protected Path mGradientPath = new Path();


	/**
	 * Creates a new ShaderAreaGraphView and reads in layout parameters from
	 * XML.
	 * 
	 * @param context
	 * @param attributes
	 */
	public HistogramGraphView(Context context, AttributeSet attributes) {
		super(context, attributes);
		mLinePaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_line_colour", 0xFF007ba1));
		mLinePaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_line_width", 3.0f));
		mLinePaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_line_anti_alias", true));
		mLinePaint.setStyle(Paint.Style.STROKE);
		
		String bitmapColoursString = attributes.getAttributeValue(customSchemaLocation, "graph_shaded_area_colours");
        if (bitmapColoursString == null) bitmapColoursString = "FF007ba1; FF007ba1; FF007ba1; 00007ba1; 00007ba1; 00007ba1";
        String [] bitmapColoursStringArray = bitmapColoursString.split("; ");
        int[] bitmapColoursIntArray = new int[bitmapColoursStringArray.length];
        for (int i = 0; i < bitmapColoursIntArray.length; i++) {
            bitmapColoursIntArray[i] = (int) (Long.parseLong(bitmapColoursStringArray[i], 16) & 0xffffffffL);
        }
        bitmapColours = bitmapColoursIntArray;

		mBitmap = Bitmap.createBitmap(bitmapColours, 0, 1, 1, 6, Bitmap.Config.ARGB_8888);
		bandShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
	}

	@Override
	protected void drawPlot(Canvas canvas) {
		for (int set = 0; set < mData.length; set++) {
			try {
				int setColour = mDataSetColours[set];
				mLinePaint.setColor(setColour);
				int[] barBitmapColours = {setColour, setColour, setColour, 00000000, 00000000, 00000000};
				Bitmap barShaderBitmap = Bitmap.createBitmap(barBitmapColours, 0, 1, 1, 6, Bitmap.Config.ARGB_8888);
				bandShader = new BitmapShader(barShaderBitmap, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
				gradientShader = new LinearGradient(0, 0, 0, graphHeight, 0xCCffffff , 0x11ffffff, Shader.TileMode.CLAMP);
				compositeShader = new ComposeShader(bandShader, gradientShader, PorterDuff.Mode.MULTIPLY);
				mAscendingPaint.setShader(compositeShader);	
			} catch (IndexOutOfBoundsException e) {
				// Just carry on using the last colour
			}
			
			float[] startCoordinates = calcCoordinates(set, 0);
			
			canvas.save();
			canvas.clipRect(mLeftPadding, mTopPadding - mLinePaint.getStrokeWidth(), mLeftPadding + graphWidth, mTopPadding + graphHeight - mAxesPaint.getStrokeWidth() / 2);
			
			mLinePath = new Path();
			mGradientPath = new Path();
			
			mLinePath.moveTo(startCoordinates[0], mTopPadding + graphHeight);
			mGradientPath.moveTo(startCoordinates[0], mTopPadding + graphHeight);
			
			for (int i = 0; i < mData[set][0].length - 1; i++) {
				float[] coords = calcCoordinates(set, i);
				if (Float.isNaN(mData[set][1][i + 1])) {
					mLinePath.lineTo(coords[0], mTopPadding + graphHeight);
					mGradientPath.lineTo(coords[0], mTopPadding + graphHeight);
				} else if (Float.isNaN(mData[set][1][i]) && Float.isNaN(mData[set][1][i + 1]) == false) {
					mLinePath.moveTo(coords[0], mTopPadding + graphHeight);
					mLinePath.lineTo(coords[0], coords[3]);
					mLinePath.lineTo(coords[2], coords[3]);
					mGradientPath.lineTo(coords[0], mTopPadding + graphHeight);
					mGradientPath.lineTo(coords[0], coords[3]);
					mGradientPath.lineTo(coords[2], coords[3]);
				} else if (coords[3] < coords[1]) {				// If the data is increasing, draw in the ascending style...
					drawAscendingSection(canvas, coords);
				} else {										// ...otherwise, draw in the descending style
					drawDescendingSection(canvas, coords);
				}
			}
			
			float[] endCoordinates = calcCoordinates(set, mData[set][0].length - 2);
			mLinePath.lineTo(endCoordinates[2], mTopPadding + graphHeight);
			mGradientPath.lineTo(endCoordinates[2], mTopPadding + graphHeight);
			mGradientPath.lineTo(startCoordinates[0], mTopPadding + graphHeight);
			mGradientPath.lineTo(startCoordinates[0], startCoordinates[1]);
			
			gradientShader = new LinearGradient(0, 0, 0, graphHeight, 0xCCffffff , 0x11ffffff, Shader.TileMode.CLAMP);
			compositeShader = new ComposeShader(bandShader, gradientShader, PorterDuff.Mode.MULTIPLY);

			mAscendingPaint.setShader(compositeShader);
			
			canvas.drawPath(mGradientPath, mBackgroundPaint);
			canvas.drawPath(mGradientPath, mAscendingPaint);
			canvas.drawPath(mLinePath, mLinePaint);
			
			canvas.restore();
		}
	}

	@Override
	protected void drawAscendingSection(Canvas canvas, float[] coords) {
		mLinePath.lineTo(coords[0], coords[3]);
		mLinePath.lineTo(coords[2], coords[3]);
		mGradientPath.lineTo(coords[0], coords[3]);
		mGradientPath.lineTo(coords[2], coords[3]);
	}

	@Override
	protected void drawDescendingSection(Canvas canvas, float[] coords) {		
		drawAscendingSection(canvas, coords);
	}
}