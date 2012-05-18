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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Displays data as a heatmap. Colours, labels and other layout features are
 * read in from XML.
 */
public class HeatMapView extends View {
	private float[][] mData = {{Float.NaN,Float.NaN}, {Float.NaN, Float.NaN}};	// Each element is a row of integer data
	protected Paint mBlockPaint = new Paint();
	protected Paint mBlockGridPaint = new Paint();
	protected int[] mColours;
	protected int[] mColourBandUpperBounds;
	protected float mTopPadding;
	protected float mBottomPadding;
	protected float mLeftPadding;
	protected float mRightPadding;
	protected float graphHeight;
	protected float graphWidth;
	protected float mTickLength;
	protected float mXLabelSpacing;
	protected float mYLabelSpacing;
	protected Paint mTextPaint = new Paint();
	protected String[] mYAxisLabels;
	protected float[] mYAxisLabelPositions;
	protected String[] mXAxisLabels;
	protected float[] mXAxisLabelPositions;
	protected int mNaNColour;
	
	/**
	 * Creates a new HeatMapView and sets layout parameters as defined by XML.
	 * 
	 * @param context
	 * @param attributes
	 */
	public HeatMapView(Context context, AttributeSet attributes) {
		super(context, attributes);
		mBlockGridPaint.setColor(0xFF000000);
		mTextPaint.setColor(0xFF000000);
		mTextPaint.setTextSize(20);
		mTextPaint.setAntiAlias(true);
		
		//String customSchemaLocation = getResources().getString(R.string.NS);
		String customSchemaLocation = "http://schemas.android.com/apk/res/net.snowdon";
        
		
		mTopPadding = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_top_padding", 30.0f);
		mBottomPadding = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_bottom_padding", 0.0f);
		mLeftPadding = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_left_padding", 50.0f);
		mRightPadding = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_right_padding", 25.0f);
		
		mTickLength = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_tick_length", 5.0f);
		mXLabelSpacing = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_x_label_spacing", 10.0f);
		mYLabelSpacing = attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_y_label_spacing", 5.0f);
		
		String yAxisLabels = attributes.getAttributeValue(customSchemaLocation, "heat_map_y_axis_labels");
		if (yAxisLabels == null) yAxisLabels = "0%; 25%; 50%; 75%; 100%";
		mYAxisLabels = yAxisLabels.split("; ");
		
		String yAxisLabelPositions = attributes.getAttributeValue(customSchemaLocation, "heat_map_y_axis_label_positions");
		if (yAxisLabelPositions == null) yAxisLabelPositions = "0.00; 1.00; 2.00; 3.00; 4.00; 5.00; 6.00";
		String [] yAxisLabelPositionsStringArray = yAxisLabelPositions.split("; ");
		float[] yAxisLabelPositionsFloatArray = new float[yAxisLabelPositionsStringArray.length];
		for (int i = 0; i < yAxisLabelPositionsFloatArray.length; i++) {
			yAxisLabelPositionsFloatArray[i] = Float.parseFloat(yAxisLabelPositionsStringArray[i]);
		}
		mYAxisLabelPositions = yAxisLabelPositionsFloatArray;
		
		String xAxisLabels = attributes.getAttributeValue(customSchemaLocation, "heat_map_x_axis_labels");
		if (xAxisLabels == null) xAxisLabels = "0%; 25%; 50%; 75%; 100%";
		mXAxisLabels = xAxisLabels.split("; ");
		
		String xAxisLabelPositions = attributes.getAttributeValue(customSchemaLocation, "heat_map_x_axis_label_positions");
		if (xAxisLabelPositions == null) xAxisLabelPositions = "0.00; 6.00; 12.00; 18.00; 24.00";
		String [] xAxisLabelPositionsStringArray = xAxisLabelPositions.split("; ");
		float[] xAxisLabelPositionsFloatArray = new float[xAxisLabelPositionsStringArray.length];
		for (int i = 0; i < xAxisLabelPositionsFloatArray.length; i++) {
			xAxisLabelPositionsFloatArray[i] = Float.parseFloat(xAxisLabelPositionsStringArray[i]);
		}
		mXAxisLabelPositions = xAxisLabelPositionsFloatArray;
		
		
		String colours = attributes.getAttributeValue(customSchemaLocation, "heat_map_colours");
		if (colours == null) colours = "FFeefbff; FFdbf7ff; FFbff0ff; FF80e1ff; FF40d2ff; FF00c3ff; FF00ace0; FF0093bf; FF007ba1; FF006280";
		String[] coloursStringArray = colours.split("; ");
		int[] coloursIntArray = new int[coloursStringArray.length];
		for (int i = 0; i < coloursIntArray.length; i++) {
			coloursIntArray[i] = (int) (Long.parseLong(coloursStringArray[i], 16) & 0xffffffffL);
		}
		mColours = coloursIntArray;
		
		mNaNColour = attributes.getAttributeIntValue(customSchemaLocation, "heat_map_no_data_colour", 0xFFAEAEAE);
		
		String colourBandUpperBounds = attributes.getAttributeValue(customSchemaLocation, "heat_map_colour_upper_bounds");
		if (colourBandUpperBounds == null) colourBandUpperBounds = "10; 20; 30; 40; 50; 60; 70; 80; 90";
		String[] colourBandUpperBoundsStringArray = colourBandUpperBounds.split("; ");
		int[] colourBandUpperBoundsIntArray = new int[colourBandUpperBoundsStringArray.length];
		for (int i = 0; i < colourBandUpperBoundsIntArray.length; i++) {
			colourBandUpperBoundsIntArray[i] = Integer.parseInt(colourBandUpperBoundsStringArray[i]);
		}
		mColourBandUpperBounds = colourBandUpperBoundsIntArray;
	}
	
	/**
     * Invalidates the view and causes it to be redrawn.
     */
    public void redraw() {
    	invalidate();
    }

	/**
	 * Sets the data to be plotted in the heat map.
	 * 
	 * @param data A two-dimensional array of floats, with the x data located
	 * in the first element and y in the second.
	 */
	public void setData(float[][] data) {
		if (data != null) {
			mData = data;
		}
	}
	
    /**
     * Sets the labels on the y-axis using the specified string array.
     * 
     * @param yLabels A string array containing the text to be displayed at each
     *            row.
     */
	public void setYLabels(String[] yLabels) {
        mYAxisLabels = yLabels;
    }
	
	@Override
	protected void onDraw(Canvas canvas) {
		float blockWidth = (getWidth() - mLeftPadding - mRightPadding - mBlockPaint.getStrokeWidth()) / mData[0].length;
		float blockHeight = (getHeight() - mTopPadding - mBottomPadding - mBlockPaint.getStrokeWidth()) / mData.length;
		
		float textVertOffset = 0.25f * (2 * mTextPaint.getTextSize() - mTextPaint.getFontSpacing());
		float textHoriOffset;
		for (int i = 0; i < mXAxisLabels.length; i++) {
			textHoriOffset = mTextPaint.measureText(mXAxisLabels[i]) / 2;
			canvas.drawText(mXAxisLabels[i], mLeftPadding - textHoriOffset + mXAxisLabelPositions[i] * blockWidth - 0.5f, mTopPadding - mXLabelSpacing, mTextPaint);
			canvas.drawLine(mLeftPadding + mXAxisLabelPositions[i] * blockWidth - 0.5f, mTopPadding - 0.5f, mLeftPadding + mXAxisLabelPositions[i] * blockWidth - 0.5f, mTopPadding - mTickLength - 0.5f, mBlockGridPaint);
		}
		
		for (int i = 0; i < mYAxisLabels.length; i++) {
			textHoriOffset = mTextPaint.measureText(mYAxisLabels[i]);
			canvas.drawText(mYAxisLabels[i], mLeftPadding - textHoriOffset - mYLabelSpacing, mTopPadding + textVertOffset + blockHeight * (0.5f + mYAxisLabelPositions[i]), mTextPaint);
		}
		
		for (int j = 0; j < mData.length; j++) {
			for (int i = 0; i < mData[j].length; i++) {
				int colourIndex = 0;
				if (Float.isNaN(mData[j][i]) == true) {
					mBlockPaint.setColor(mNaNColour);
				} else {
					for (int k = 0; k < mColourBandUpperBounds.length; k++) {
						if (mData[j][i] > mColourBandUpperBounds[k]) {
							colourIndex = k + 1;
						}
					}			
					mBlockPaint.setColor(mColours[colourIndex]);
				}
				canvas.drawRect(mLeftPadding + i * blockWidth, mTopPadding + j * blockHeight, mLeftPadding + (i + 1) * blockWidth, mTopPadding + (j + 1) * blockHeight, mBlockPaint);
				canvas.drawLine(mLeftPadding + i * blockWidth - 0.5f, mTopPadding + j * blockHeight - 0.5f, mLeftPadding + (i + 1) * blockWidth - 0.5f, mTopPadding + j * blockHeight - 0.5f, mBlockGridPaint);
				canvas.drawLine(mLeftPadding + i * blockWidth - 0.5f, mTopPadding + (j + 1) * blockHeight - 0.5f, mLeftPadding + (i + 1) * blockWidth - 0.5f, mTopPadding + (j + 1) * blockHeight - 0.5f, mBlockGridPaint);
				canvas.drawLine(mLeftPadding + i * blockWidth - 0.5f, mTopPadding + j * blockHeight - 0.5f, mLeftPadding + (i) * blockWidth - 0.5f, mTopPadding + (j + 1) * blockHeight - 0.5f, mBlockGridPaint);
				canvas.drawLine(mLeftPadding + (i + 1) * blockWidth - 0.5f, mTopPadding + j * blockHeight - 0.5f, mLeftPadding + (i + 1) * blockWidth - 0.5f, mTopPadding + (j + 1) * blockHeight - 0.5f, mBlockGridPaint);
			}
		}
	}
}
