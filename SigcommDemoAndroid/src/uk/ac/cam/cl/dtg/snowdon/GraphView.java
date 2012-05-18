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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * GraphView is an abstract base class for graphs in the library. It provides
 * methods for setting and binning data (as appropriate), the calculation of
 * points, and the drawing of axes and gridlines. The actual drawing of the data
 * is handled by methods in derived classes. Many of the layout parameters are
 * specified in XML files, the loading of which is handled here (except for
 * class-specific layout features)
 */
public abstract class GraphView extends View {
    //---------- Data ----------\\
    // We need two zeroes in each so that calcCoordinates does not fail
    protected float[][][] mData = {{{0, 0}, {0, 0}}};
    // Flag to prevent binning already-binned data
    protected boolean mDataBinned = false;
    protected float mMaxX;
    protected float mMinX;
    protected float mMaxY;
    protected float mMinY;
    protected float mDataXWidth;
    protected float mDataYHeight;

    //---------- Padding and layout ----------\\
    protected float mTopPadding;
    protected float mBottomPadding;
    protected float mLeftPadding;
    protected float mRightPadding;
    protected float mGridlineLabelHoriSpacing;
    protected float mGridlineLabelVertSpacing;
    protected float graphHeight;
    protected float graphWidth;

    //---------- Paints, colours and sizes ----------\\
    protected String[] mYAxisLabels;
    protected float[] mYAxisLabelPositions;
    protected String[] mXAxisLabels;
    protected float[] mXAxisLabelPositions;
    protected float[] mXTickPositions;
    protected float mXTickLength = 5.0f;
    protected float[] mYTickPositions;
    protected float mYTickLength = 5.0f;
    protected Paint mAxesPaint = new Paint();
    protected boolean mAxesPaintAntiAlias;
    protected Paint mGridlinesPaint = new Paint();
    
    protected String mXAxisLabel;
    protected String mYAxisLabel;
    protected Paint mAxisLabelPaint = new Paint();
    protected float mAxisLabelTextSize;
    protected float mXAxisLabelOffset;
    protected float mYAxisLabelOffset;

    protected Paint mAscendingPaint = new Paint();
    protected Paint mDescendingPaint = new Paint();

    protected Paint mBackgroundPaint = new Paint();
    protected Paint mOverlay1Paint = new Paint();
    protected boolean mDrawOverlay1;
    protected String mOverlay1Text;
    protected float mOverlay1TextSize;
    protected float mOverlay1XPos;
    protected float mOverlay1YPos;
    protected Paint mOverlay2Paint = new Paint();
    protected boolean mDrawOverlay2;
    protected String mOverlay2Text;
    protected float mOverlay2TextSize;
    protected float mOverlay2XPos;
    protected float mOverlay2YPos;
    
	protected int[] mDataSetColours = {0xFF007ba1, 0xFF0f7231, 0xFF720f31};

    //---------- XML ----------\\
    // Used to read custom graph layout
    protected String customSchemaLocation;

	/**
	 * Creates a new GraphView and sets layout parameters based on values
	 * specified in XML.
	 * 
	 * @param context
	 * @param attributes
	 */
    public GraphView(Context context, AttributeSet attributes) {
        super(context, attributes);

        // Dodgy hack for using correct XML definitions in a library
        //customSchemaLocation = getResources().getString(R.string.NS);
        customSchemaLocation = "http://schemas.android.com/apk/res/net.snowdon";
        mTopPadding = attributes.getAttributeFloatValue(customSchemaLocation, "graph_top_padding", 20.0f);
        mBottomPadding = attributes.getAttributeFloatValue(customSchemaLocation, "graph_bottom_padding", 60.0f);
        mLeftPadding = attributes.getAttributeFloatValue(customSchemaLocation, "graph_left_padding", 60.0f);
        mRightPadding = attributes.getAttributeFloatValue(customSchemaLocation, "graph_right_padding", 30.0f);
        mGridlineLabelHoriSpacing = attributes.getAttributeFloatValue(customSchemaLocation, "graph_gridline_label_hori_spacing", 10.0f);
        mGridlineLabelVertSpacing = attributes.getAttributeFloatValue(customSchemaLocation, "graph_gridline_label_vert_spacing", 10.0f);
        
        // TODO: Use an ArrayList of overlays or something, so that we can have as many different ones as we want. Reading from XML for all of them may be difficult to implement, though.
        mDrawOverlay1 = attributes.getAttributeBooleanValue(customSchemaLocation, "graph_draw_overlay1", false);
        mOverlay1Paint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_overlay1_colour", 0xFFffffff));
        mOverlay1Text = attributes.getAttributeValue(customSchemaLocation, "graph_overlay1_text");
        if (mOverlay1Text == null) mOverlay1Text = "";
        mOverlay1TextSize = attributes.getAttributeFloatValue(customSchemaLocation, "graph_overlay1_text_size", 40.0f);
        mOverlay1Paint.setTextSize(mOverlay1TextSize);
        mOverlay1Paint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_overlay1_anti_alias", true));
        mOverlay1XPos = attributes.getAttributeFloatValue(customSchemaLocation, "graph_overlay1_x_pos", 0.75f);
        mOverlay1YPos = attributes.getAttributeFloatValue(customSchemaLocation, "graph_overlay1_y_pos", 0.25f);
        
        mDrawOverlay2 = attributes.getAttributeBooleanValue(customSchemaLocation, "graph_draw_overlay2", false);
        mOverlay2Paint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_overlay2_colour", 0xFFffffff));
        mOverlay2Text = attributes.getAttributeValue(customSchemaLocation, "graph_overlay2_text");
        if (mOverlay2Text == null) mOverlay2Text = "";
        mOverlay2TextSize = attributes.getAttributeFloatValue(customSchemaLocation, "graph_overlay2_text_size", 40.0f);
        mOverlay2Paint.setTextSize(mOverlay2TextSize);
        mOverlay2Paint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_overlay2_anti_alias", true));
        mOverlay2XPos = attributes.getAttributeFloatValue(customSchemaLocation,"graph_overlay2_x_pos", 0.75f);
        mOverlay2YPos = attributes.getAttributeFloatValue(customSchemaLocation,"graph_overlay2_y_pos", 0.25f);

        String yAxisLabels = attributes.getAttributeValue(customSchemaLocation, "graph_y_axis_labels");
        if (yAxisLabels == null) yAxisLabels = "0%; 25%; 50%; 75%; 100%";
        mYAxisLabels = yAxisLabels.split("; ");
        String yAxisLabelPositions = attributes.getAttributeValue(customSchemaLocation, "graph_y_axis_label_positions");
        if (yAxisLabelPositions == null) yAxisLabelPositions = "0.00; 0.25; 0.50; 0.75; 1.00";
        String[] yAxisLabelPositionsStringArray = yAxisLabelPositions.split("; ");
        float[] yAxisLabelPositionsFloatArray = new float[yAxisLabelPositionsStringArray.length];
        for (int i = 0; i < yAxisLabelPositionsFloatArray.length; i++) {
            yAxisLabelPositionsFloatArray[i] = Float.parseFloat(yAxisLabelPositionsStringArray[i]);
        }
        mYAxisLabelPositions = yAxisLabelPositionsFloatArray;
        
        String yTickPositions = attributes.getAttributeValue(customSchemaLocation, "graph_y_tick_positions");
        if (yTickPositions == null) yTickPositions = "0.00; 0.25; 0.50; 0.75; 1.00";
        String[] yTickPositionsStringArray = yTickPositions.split("; ");
        float[] yTickPositionsFloatArray = new float[yTickPositionsStringArray.length];
        for (int i = 0; i < yTickPositionsFloatArray.length; i++) {
            yTickPositionsFloatArray[i] = Float.parseFloat(yTickPositionsStringArray[i]);
        }
        mYTickPositions = yTickPositionsFloatArray;

        String xAxisLabels = attributes.getAttributeValue(customSchemaLocation, "graph_x_axis_labels");
        if (xAxisLabels == null) xAxisLabels = "0%; 25%; 50%; 75%; 100%";
        mXAxisLabels = xAxisLabels.split("; ");

        String xAxisLabelPositions = attributes.getAttributeValue(customSchemaLocation, "graph_x_axis_label_positions");
        if (xAxisLabelPositions == null) xAxisLabelPositions = "0.00; 0.25; 0.50; 0.75; 1.00";
        String[] xAxisLabelPositionsStringArray = xAxisLabelPositions.split("; ");
        float[] xAxisLabelPositionsFloatArray = new float[xAxisLabelPositionsStringArray.length];
        for (int i = 0; i < xAxisLabelPositionsFloatArray.length; i++) {
            xAxisLabelPositionsFloatArray[i] = Float.parseFloat(xAxisLabelPositionsStringArray[i]);
        }
        mXAxisLabelPositions = xAxisLabelPositionsFloatArray;
        
        String xTickPositions = attributes.getAttributeValue(customSchemaLocation, "graph_x_tick_positions");
        if (xTickPositions == null) xTickPositions = "0.00; 0.25; 0.50; 0.75; 1.00";
        String[] xTickPositionsStringArray = xTickPositions.split("; ");
        float[] xTickPositionsFloatArray = new float[xTickPositionsStringArray.length];
        for (int i = 0; i < xTickPositionsFloatArray.length; i++) {
            xTickPositionsFloatArray[i] = Float.parseFloat(xTickPositionsStringArray[i]);
        }
        mXTickPositions = xTickPositionsFloatArray;
        
        
        mXAxisLabel = attributes.getAttributeValue(customSchemaLocation, "graph_x_axis_label");
        if (mXAxisLabel == null) mXAxisLabel = "";
        mYAxisLabel = attributes.getAttributeValue(customSchemaLocation, "graph_y_axis_label");
        if (mYAxisLabel == null) mYAxisLabel = "";
        mXAxisLabelOffset = attributes.getAttributeFloatValue(customSchemaLocation, "graph_x_axis_label_offset", 0.0f);
        mYAxisLabelOffset = attributes.getAttributeFloatValue(customSchemaLocation, "graph_y_axis_label_offset", 0.0f);
        
        mAxisLabelPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_axis_label_colour", 0xFFFFFFFF));
        mAxisLabelTextSize = attributes.getAttributeFloatValue(customSchemaLocation, "graph_axis_label_text_size", 20.0f);
        mAxisLabelPaint.setTextSize(mAxisLabelTextSize);
        mAxisLabelPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_axis_label_anti_alias", true));

        mBackgroundPaint.setColor(attributes.getAttributeIntValue("http://schemas.android.com/apk/res/android", "background",0x00000000));

        mAxesPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_axes_colour", 0xFFFFFFFF));
        mAxesPaint.setTextSize(attributes.getAttributeFloatValue(customSchemaLocation, "graph_axes_text_size", 20.0f));
        mAxesPaintAntiAlias = attributes.getAttributeBooleanValue(customSchemaLocation, "graph_axes_anti_alias", true);
        mAxesPaint.setAntiAlias(mAxesPaintAntiAlias);
        mAxesPaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_axes_stroke_width", 2.0f));

        mGridlinesPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_gridlines_colour", 0xFFFFFFFF));
        mGridlinesPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_gridlines_anti_alias", false));
        mGridlinesPaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_gridlines_stroke_width", 1.0f));
        mGridlinesPaint.setStrokeCap(Paint.Cap.SQUARE);

        mAscendingPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_ascending_colour", 0xFF00FF00));
        mAscendingPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_ascending_anti_alias", true));
        mAscendingPaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_ascending_stroke_width", 2.0f));

        mDescendingPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "graph_descending_colour", 0xFFFF0000));
        mDescendingPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "graph_descending_anti_alias", true));
        mDescendingPaint.setStrokeWidth(attributes.getAttributeFloatValue(customSchemaLocation, "graph_descending_stroke_width", 2.0f));

        mAxesPaint.setStyle(Paint.Style.STROKE);
        mGridlinesPaint.setStyle(Paint.Style.STROKE);
    }
    
    public void setDataSetColours(int[] colours) {
    	mDataSetColours = colours;
    }
    
	/**
	 * Invalidates the view and causes it to be redrawn.
	 */
    public void redraw() {
    	invalidate();
    }

	/**
	 * Sets the data to be plotted. Only minimal validation takes place (i.e.
	 * checking for null arrays, checking the x and y arrays are the same
	 * length). If validation fails, the method does nothing, and does return or
	 * throw anything.
	 * 
	 * @param data
	 *            A two-dimensional array, x values as the first array, y values
	 *            as the second.
	 * @param minX
	 *            The minimum value of x in the data set that should be plotted
	 *            in the first quadrant of the graph.
	 * @param maxX
	 *            The maximum value of x in the data set that should be plotted
	 *            in the first quadrant of the graph.
	 * @param minY
	 *            The minimum value of y in the data set that should be plotted
	 *            in the first quadrant of the graph.
	 * @param maxY
	 *            The maximum value of y in the data set that should be plotted
	 *            in the first quadrant of the graph.
	 */
    public void setData(float[][][] data, float minX, float maxX, float minY, float maxY) {
        if (data != null && data[0][0].length > 1) {
            mData = data;
            mMinX = minX;
            mMaxX = maxX;
            mDataXWidth = mMaxX - mMinX;
            mMinY = minY;
            mMaxY = maxY;
            mDataYHeight = mMaxY - mMinY;
            mDataBinned = false;
        } else {
        	throw new IllegalArgumentException();
        }
    }
    
	/**
	 * Returns the data that has been set. Includes data that has been added
	 * using addData().
	 * 
	 * @return Three-dimensional float array, with the first dimension being
	 *         sets of data, the second x values and the third y values.
	 */
    public float[][][] getData() {
    	return mData;
    }
    
	/**
	 * Add a two-dimensional array of floats to the data already set using
	 * setData(). The first dimension should be x values, and the second should
	 * be y values. Insert NaNs in the y values for gaps in the data.
	 * 
	 * @param additionalData
	 *            The data to be added.
	 * @param minX
	 *            The new minimum x value in the data set. Use NaN if you wish
	 *            to keep the value previously set using setData().
	 * @param maxX
	 *            The new maximum x value in the data set. Use NaN if you wish
	 *            to keep the value previously set using setData().
	 * @param minY
	 *            The new minimum y value in the data set. Use NaN if you wish
	 *            to keep the value previously set using setData().
	 * @param maxY
	 *            The new maximum y value in the data set. Use NaN if you wish
	 *            to keep the value previously set using setData().
	 */
    public void addData(float[][] additionalData, float minX, float maxX, float minY, float maxY) {
    	List<float[][]> mDataList = new ArrayList<float[][]>();
    	for (int set = 0; set < mData.length; set++) {
    		mDataList.add(mData[set]);
    	}
    	mDataList.add(additionalData);
    	
    	float[][][] newMData = new float[mDataList.size()][2][];
    	for (int set = 0; set < mDataList.size(); set++) {
    		newMData[set][0] = mDataList.get(set)[0];
    		newMData[set][1] = mDataList.get(set)[1];
    	}
    	mData = newMData;
    	
    	if(!Float.isNaN(minX)) mMinX = minX;
    	if(!Float.isNaN(maxX)) mMaxX = maxX;
    	if(!Float.isNaN(minY)) mMinY = minY;
    	if(!Float.isNaN(maxY)) mMaxY = maxY;
    	
    	mDataXWidth = mMaxX - mMinX;
    	mDataYHeight = mMaxY - mMinY;
    }
    
	/**
	 * Sets the padding of the graph. To maintain the current padding value for
	 * a side, input a NaN as the argument.
	 * 
	 * @param topPadding
	 *            Top padding measured in pixels.
	 * @param bottomPadding
	 *            Bottom padding measured in pixels.
	 * @param leftPadding
	 *            Left padding measured in pixels.
	 * @param rightPadding
	 *            Right padding measured in pixels.
	 */
    public void setPadding(float topPadding, float bottomPadding, float leftPadding, float rightPadding) {
    	if (!Float.isNaN(topPadding)) mTopPadding = topPadding;
    	if (!Float.isNaN(bottomPadding)) mBottomPadding = bottomPadding;
    	if (!Float.isNaN(leftPadding)) mLeftPadding = leftPadding;
    	if (!Float.isNaN(rightPadding)) mRightPadding = rightPadding;
    }
    
    public void setXAxisLabel(String label) {
    	mXAxisLabel = label;
    }
    
    public void setYAxisLabel(String label) {
    	mYAxisLabel = label;
    }

	/**
	 * Sets the labels on the x-axis using the specified string array.
	 * 
	 * @param xLabels
	 *            A string array containing the text to be displayed at each
	 *            position as specified by the array passed to
	 *            setXLabelPositions. "" strings can be used for the appearance
	 *            of unlabelled gridlines.
	 */
    public void setXLabels(String[] xLabels) {
        mXAxisLabels = xLabels;
    }

	/**
	 * Sets the position of labels (and gridlines) on the x-axis. Specified as
	 * fractions of the total width (i.e. 0.5 would place a gridline half-way
	 * along the x-axis). Use in conjunction with setXLabels to display text
	 * labels on gridlines.
	 * 
	 * @param xLabelPositions
	 *            A float array containing the fractional positions of each
	 *            gridline and corresponding label.
	 */
    public void setXLabelPositions(float[] xLabelPositions) {
        mXAxisLabelPositions = xLabelPositions;
    }
    
	/**
	 * Sets the positions of ticks along the x-axis. Specified as fractions of
	 * the total width (i.e. 0.5 would place a tick half-way along the x-axis).
	 * 
	 * @param xTickPositions
	 */
    public void setXTickPositions(float[] xTickPositions) {
    	mXTickPositions = xTickPositions;
    }

	/**
	 * Sets the labels on the y-axis using the specified string array.
	 * 
	 * @param yLabels
	 *            A string array containing the text to be displayed at each
	 *            position as specified by the array passed to
	 *            setYLabelPositions. "" strings can be used for the appearance
	 *            of unlabelled gridlines.
	 */
    public void setYLabels(String[] yLabels) {
        mYAxisLabels = yLabels;
    }

	/**
	 * Sets the position of labels (and gridlines) on the y-axis. Specified as
	 * fractions of the total height (i.e. 0.5 would place a gridline half-way
	 * along the y-axis). Use in conjunction with setYLabels to display text
	 * labels on gridlines.
	 * 
	 * @param yLabelPositions
	 *            A float array containing the fractional positions of each
	 *            gridline and corresponding label.
	 */
    public void setYLabelPositions(float[] yLabelPositions) {
        mYAxisLabelPositions = yLabelPositions;
    }
    
	/**
	 * Sets the positions of ticks along the y-axis. Specified as fractions of
	 * the total height (i.e. 0.5 would place a tick half-way along the y-axis).
	 * 
	 * @param yTickPositions
	 */
    public void setYTickPositions(float[] yTickPositions) {
    	mYTickPositions = yTickPositions;
    }
    
	/**
	 * Sets the colour of all ascending lines (if the derived class makes a
	 * distinction).
	 * 
	 * @param color
	 *            The RBG color value
	 */
    public void setAscendingPaintColor(int color) {
    	mAscendingPaint.setColor(color);
    }

	/**
	 * Sets the colour of all descending lines (if the derived class makes a
	 * distinction).
	 * 
	 * @param color
	 *            The RBG color value
	 */
    public void setDescendingPaintColor(int color) {
    	mDescendingPaint.setColor(color);
    }
    
    /**
	 * Sets the colour of the axis labels.
	 * 
	 * @param color
	 *            The RBG color value
	 */
    public void setAxisLabelPaintColour(int color) {
    	mAxisLabelPaint.setColor(color);
    }
    
	/**
	 * Sets the text size of the axis labels.
	 * 
	 * @param size
	 *            The size of the text in pixels
	 */
    public void setAxisLabelTextSize(float size) {
    	mAxisLabelPaint.setTextSize(size);
    }

	/**
	 * Sets whether the axis labels are anti-aliased.
	 * 
	 * @param antialias
	 *            True for anti-aliasing on.
	 */
    public void setAxisLabelAntiAlias(boolean antialias) {
    	mAxisLabelPaint.setAntiAlias(antialias);
    }
    
    protected void binData(float binWidth) {
    	for (int i = 0; i < mData.length; i++) {
    		mData[i] = DataUtilities.binFloatFloatArray(mData[i], binWidth, mMinX, mMaxX);
    	}
    	mDataBinned = true; // Ensures the data is not binned again
    }

    @Override
	public void onDraw(Canvas canvas) {    	
        graphWidth = getWidth() - mLeftPadding - mRightPadding;
        graphHeight = getHeight() - mTopPadding - mBottomPadding;

        if (mDataBinned != true && mData[0][0].length > graphWidth) {
        	binData(mDataXWidth / graphWidth);
        }

        for (int i = 0; i < mData.length; i++) {
        	drawGridlines(canvas);
        	drawAxes(canvas);
        	drawPlot(canvas);
        	drawXTicks(canvas);
        	drawYTicks(canvas);
        	drawAxesLabels(canvas);
        	if (mDrawOverlay1 == true) {
        		drawOverlays(canvas);
        	}
        }
    }
    
    protected void drawXTicks(Canvas canvas) {
    	Path tickPath = new Path();

    	for (int i = 0; i < mXTickPositions.length; i++) {
    		tickPath.moveTo(mLeftPadding + (mXTickPositions[i]) * graphWidth, mTopPadding + graphHeight);
    		tickPath.lineTo(mLeftPadding + (mXTickPositions[i]) * graphWidth, mTopPadding + graphHeight + mXTickLength);
    	}

    	mAxesPaint.setStyle(Paint.Style.STROKE);
    	mAxesPaint.setAntiAlias(false);
    	canvas.drawPath(tickPath, mAxesPaint);
    	mAxesPaint.setAntiAlias(mAxesPaintAntiAlias);
    	mAxesPaint.setStyle(Paint.Style.FILL);
    }
    
    protected void drawYTicks(Canvas canvas) {
    	Path tickPath = new Path();
    	
    	for (int i = 0; i < mYTickPositions.length; i++) {
            tickPath.moveTo(mLeftPadding - mYTickLength, mTopPadding + (1.00f - mYTickPositions[i]) * graphHeight);
            tickPath.lineTo(mLeftPadding + mAxesPaint.getStrokeWidth() / 2, mTopPadding + (1.00f - mYTickPositions[i]) * graphHeight);
        }
    	mAxesPaint.setStyle(Paint.Style.STROKE);
        mAxesPaint.setAntiAlias(false);
        canvas.drawPath(tickPath, mAxesPaint);
        mAxesPaint.setAntiAlias(mAxesPaintAntiAlias);
        mAxesPaint.setStyle(Paint.Style.FILL);
    }
    
    protected void drawAxesLabels(Canvas canvas) {
    	canvas.drawText(mXAxisLabel, mLeftPadding + graphWidth / 2 - mAxisLabelPaint.measureText(mXAxisLabel) / 2, mTopPadding + graphHeight + mBottomPadding - mXAxisLabelOffset, mAxisLabelPaint);
    	canvas.rotate(-90, 0, 0);
    	canvas.drawText(mYAxisLabel, -(mTopPadding + graphHeight / 2 + mAxisLabelPaint.measureText(mYAxisLabel) / 2), mAxisLabelPaint.getTextSize() + mYAxisLabelOffset, mAxisLabelPaint);
    	canvas.rotate(90, 0, 0);
    }

    protected void drawGridlines(Canvas canvas) {
        Path gridlinePath = new Path();

        for (int i = 0; i < mYAxisLabelPositions.length; i++) {
            gridlinePath.moveTo(mLeftPadding, mTopPadding + (1.00f - mYAxisLabelPositions[i]) * graphHeight);
            gridlinePath.lineTo(mLeftPadding + graphWidth, mTopPadding + (1.00f - mYAxisLabelPositions[i]) * graphHeight);
        }
        for (int i = 0; i < mXAxisLabelPositions.length; i++) {
            gridlinePath.moveTo(mLeftPadding + (mXAxisLabelPositions[i]) * graphWidth, mTopPadding);
            gridlinePath.lineTo(mLeftPadding + (mXAxisLabelPositions[i]) * graphWidth, mTopPadding + graphHeight);
        }
        
        canvas.drawPath(gridlinePath, mGridlinesPaint);
    }

    protected void drawAxes(Canvas canvas) {
        float textHoriOffset;
        float textVertOffset = -(2 * mAxesPaint.getTextSize() - mAxesPaint .getFontSpacing()) / 2;

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
            canvas.drawText(mXAxisLabels[i], mLeftPadding - textHoriOffset + mXAxisLabelPositions[i] * graphWidth, mTopPadding + graphHeight + textVertOffset, mAxesPaint);
        }
    }

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
    	}
    }

	/**
	 * Sets the text to be drawn in the first overlay overlay.
	 * 
	 * @param text
	 *            The string to be drawn in the overlay.
	 */
    public void setOverlay1Text(String text) {
        mOverlay1Text = text;
        mDrawOverlay1 = true;
    }
    
	/**
	 * Sets the text to be drawn in the second overlay overlay.
	 * 
	 * @param text
	 *            The string to be drawn in the overlay.
	 */
	public void setOverlay2Text(String text) {
		mOverlay2Text = text;
		mDrawOverlay2 = true;
	}

    /**
     * Sets the text to be drawn in the first overlay, at the fractional coordinates
     * specified.
     * 
     * @param text The string to be drawn in the overlay.
     * @param overlayXPos The fractional coordinate in the x-direction (i.e. 0.5
     *            would position the start of the text half-way along the
     *            x-axis).
     * @param overlayYPos The fractional coordinate in the y-direction.
     */
    public void setOverlay1Text(String text, float overlayXPos, float overlayYPos) {
        mOverlay1Text = text;
        mDrawOverlay1 = true;
        mOverlay1XPos = overlayXPos;
        mOverlay1YPos = overlayYPos;
    }
    
	/**
	 * Sets the text to be drawn in the second overlay, at the fractional
	 * coordinates specified.
	 * 
	 * @param text
	 *            The string to be drawn in the overlay.
	 * @param overlayXPos
	 *            The fractional coordinate in the x-direction (i.e. 0.5 would
	 *            position the start of the text half-way along the x-axis).
	 * @param overlayYPos
	 *            The fractional coordinate in the y-direction.
	 */
    public void setOverlay2Text(String text, float overlayXPos, float overlayYPos) {
        mOverlay2Text = text;
        mDrawOverlay2 = true;
        mOverlay2XPos = overlayXPos;
        mOverlay2YPos = overlayYPos;
    }

    protected void drawOverlays(Canvas canvas) {
        canvas.drawText(mOverlay1Text, mLeftPadding + mOverlay1XPos * graphWidth, mTopPadding + (1.0f - mOverlay1YPos) * graphHeight, mOverlay1Paint);
        canvas.drawText(mOverlay2Text, mLeftPadding + mOverlay2XPos * graphWidth, mTopPadding + (1.0f - mOverlay2YPos) * graphHeight, mOverlay2Paint);
    }

    protected float[] calcCoordinates(int set, int index) {
        float startX;
        float startY;
        float endX;
        float endY;

        startX = mLeftPadding + (mData[set][0][index] - mMinX) * graphWidth / mDataXWidth;
        endX = mLeftPadding + (mData[set][0][index + 1] - mMinX) * graphWidth / mDataXWidth;

        if(Float.isNaN(mData[set][1][index])) {
        	// Note that unless drawPlot is overridden, we should just skip over this anyway when we come to draw it
        	startY = mTopPadding + graphHeight;
        } else {
        	startY = mTopPadding + graphHeight - (mData[set][1][index] - mMinY) * graphHeight / mDataYHeight;
        }

        if(Float.isNaN(mData[set][1][index + 1])) {
        	endY = mTopPadding + graphHeight;
        } else {
        	endY = mTopPadding + graphHeight - (mData[set][1][index + 1] - mMinY) * graphHeight / mDataYHeight;
        }

        float[] coords = {startX, startY, endX, endY};
        return coords;
    }

    // This will be implemented in child classes
    protected abstract void drawDescendingSection(Canvas canvas, float[] coords);

    // This will be implemented in child classes
    protected abstract void drawAscendingSection(Canvas canvas, float[] coords);
    
	/**
	 * Returns the width of the graph.
	 * 
	 * @return The width of the graph in pixels.
	 */
    public float getGraphWidth() {
    	return graphWidth;
    }
}
