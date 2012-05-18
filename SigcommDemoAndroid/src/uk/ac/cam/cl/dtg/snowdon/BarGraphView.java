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
 * Inherits from GraphView and draws a bar chart graph with a shaded area.
 * Layout parameters are read in from XML. When specifying the data to be
 * plotted, the x-values give the left and right edges' positions, with the y
 * value giving the height of the bar. The first y value is ignored as there is
 * one more side edge than top edge. Bars can have different colours to one
 * another by use of setBarColours().
 */
public class BarGraphView extends GraphView {
	protected Paint mLinePaint = new Paint();
	protected int[] bitmapColours;
	protected Bitmap mBitmap;
	protected Shader bandShader;
	protected Shader gradientShader;
	protected Shader compositeShader;
	protected Path mLinePath = new Path();
	protected Path mGradientPath = new Path();
	protected float barBorder;
	protected int[][] mBarColours = {{0xFF007ba1}};
	protected float mBarNumSpacing;
	protected boolean drawBarNumbers;

	public BarGraphView(Context context, AttributeSet attributes) {
		super(context, attributes);
		mLinePaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_line_colour", 0xFF007ba1));
		mLinePaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_line_width", 3.0f));
		mLinePaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_line_anti_alias", true));
		mLinePaint.setStyle(Paint.Style.STROKE);
		
		mBarNumSpacing = attributes.getAttributeFloatValue(customSchemaLocation, "graph_bar_number_spacing", 10.0f);
		drawBarNumbers = attributes.getAttributeBooleanValue(customSchemaLocation, "graph_draw_bar_numbers", false);

		barBorder = attributes.getAttributeFloatValue(customSchemaLocation, "graph_bar_border", 10.0f);

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
	
	/**
	 * Sets the colours of the bars. If there are more colours than bars, then
	 * the extra colours are ignored. If there are fewer colours than bars, then
	 * the extra bars default to a blue colour.
	 * 
	 * @param colours
	 *            An integer array specifying the colours of each bar in order
	 */
	public void setBarColours(int[][] colours) {
		mBarColours = colours;
	}
	
	public void setDrawBarNumbers(boolean value) {
		drawBarNumbers = value;
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
			canvas.save();
			canvas.clipRect(mLeftPadding, mTopPadding - mLinePaint.getStrokeWidth(), mLeftPadding + graphWidth, mTopPadding + graphHeight - mAxesPaint.getStrokeWidth() / 2);
			
			for (int i = 0; i < mData[set][0].length - 1; i++) {
				float[] coords = calcCoordinates(set, i);
				if (Float.isNaN(mData[set][1][i + 1])) {
				} else if (Float.isNaN(mData[set][1][i]) && Float.isNaN(mData[set][1][i + 1]) == false) {
				} else {
					mLinePath = new Path();
					mGradientPath = new Path();

					drawAscendingSection(canvas, coords);

					gradientShader = new LinearGradient(0, 0, 0, graphHeight, 0xCCffffff , 0x11ffffff, Shader.TileMode.CLAMP);
					compositeShader = new ComposeShader(bandShader, gradientShader, PorterDuff.Mode.MULTIPLY);

					mAscendingPaint.setShader(compositeShader);

					try {
						int barColour = mBarColours[set][i];
						mLinePaint.setColor(barColour);
						int[] barBitmapColours = {barColour, barColour, barColour, 00000000, 00000000, 00000000};
						Bitmap barShaderBitmap = Bitmap.createBitmap(barBitmapColours, 0, 1, 1, 6, Bitmap.Config.ARGB_8888);
						bandShader = new BitmapShader(barShaderBitmap, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
						gradientShader = new LinearGradient(0, 0, 0, graphHeight, 0xCCffffff , 0x11ffffff, Shader.TileMode.CLAMP);
						compositeShader = new ComposeShader(bandShader, gradientShader, PorterDuff.Mode.MULTIPLY);
						mAscendingPaint.setShader(compositeShader);
					} catch (IndexOutOfBoundsException e) {
						try {
						mLinePaint.setColor(mDataSetColours[set]);
						bandShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
						gradientShader = new LinearGradient(0, 0, 0, graphHeight, 0xCCffffff , 0x11ffffff, Shader.TileMode.CLAMP);
						compositeShader = new ComposeShader(bandShader, gradientShader, PorterDuff.Mode.MULTIPLY);

						mAscendingPaint.setShader(compositeShader);
						} catch (IndexOutOfBoundsException e2) {
							
						}
					}

					canvas.drawPath(mGradientPath, mBackgroundPaint);
					canvas.drawPath(mGradientPath, mAscendingPaint);
					canvas.drawPath(mLinePath, mLinePaint);
					
					if (drawBarNumbers) {
						canvas.drawText(Float.toString(mData[set][1][i+1]), coords[0] + (coords[2] - coords[0] - mAxisLabelPaint.measureText(Float.toString(mData[set][1][i+1]))) / 2, coords[3] - mBarNumSpacing, mAxisLabelPaint);
					}
				}
			}
			canvas.restore();
		}
	}

	@Override
	protected void drawAscendingSection(Canvas canvas, float[] coords) {
		mLinePath.moveTo(coords[0] + barBorder, mTopPadding + graphHeight);
		mLinePath.lineTo(coords[0] + barBorder, coords[3]);
		mLinePath.lineTo(coords[2] - barBorder, coords[3]);
		mLinePath.lineTo(coords[2] - barBorder, mTopPadding + graphHeight);
		mGradientPath.moveTo(coords[0] + barBorder, mTopPadding + graphHeight);
		mGradientPath.lineTo(coords[0] + barBorder, coords[3]);
		mGradientPath.lineTo(coords[2] - barBorder, coords[3]);
		mGradientPath.lineTo(coords[2] - barBorder, mTopPadding + graphHeight);
	}

	@Override
	protected void drawDescendingSection(Canvas canvas, float[] coords) {		
		drawAscendingSection(canvas, coords);
	}

	@Override
	protected void drawGridlines(Canvas canvas) {
		Path gridlinePath = new Path();

		for (int i = 0; i < mYAxisLabelPositions.length; i++) {
			gridlinePath.moveTo(mLeftPadding, mTopPadding + (1.00f - mYAxisLabelPositions[i]) * graphHeight);
			gridlinePath.lineTo(mLeftPadding + graphWidth, mTopPadding + (1.00f - mYAxisLabelPositions[i]) * graphHeight);
		}

		canvas.drawPath(gridlinePath, mGridlinesPaint);
	}

	@Override
	protected void drawAxes(Canvas canvas) {
		float textHoriOffset;
		float textVertOffset = -(2 * mAxesPaint.getTextSize() - mAxesPaint.getFontSpacing()) / 2; 

		Path axesPath = new Path();
		axesPath.moveTo(mLeftPadding, mTopPadding - mGridlinesPaint.getStrokeWidth() / 2);
		axesPath.lineTo(mLeftPadding, mTopPadding + graphHeight);
		axesPath.lineTo(mLeftPadding + graphWidth + mGridlinesPaint.getStrokeWidth() / 2, mTopPadding + graphHeight);
		mAxesPaint.setStyle(Paint.Style.STROKE);
		mAxesPaint.setAntiAlias(false);
		canvas.drawPath(axesPath, mAxesPaint);
		mAxesPaint.setAntiAlias(mAxesPaintAntiAlias);
		mAxesPaint.setStyle(Paint.Style.FILL);

		for (int i = 0; i < mYAxisLabels.length; i++) {
			textHoriOffset = mAxesPaint.measureText(mYAxisLabels[i]) + mGridlineLabelHoriSpacing;
			canvas.drawText(mYAxisLabels[i], mLeftPadding - textHoriOffset, mTopPadding - textVertOffset + (1.00f - mYAxisLabelPositions[i]) * graphHeight, mAxesPaint);
		}

		textVertOffset = (2 * mAxesPaint.getTextSize() - mAxesPaint.getFontSpacing()) + mGridlineLabelVertSpacing;

		for (int i = 0; i < mXAxisLabels.length; i++) {
			textHoriOffset = mAxesPaint.measureText(mXAxisLabels[i]) / 2;
			canvas.drawText(mXAxisLabels[i], mLeftPadding - textHoriOffset + (mXAxisLabelPositions[i] - mMinX) * graphWidth / mDataXWidth, mTopPadding + graphHeight + textVertOffset, mAxesPaint);
		}
	}
}