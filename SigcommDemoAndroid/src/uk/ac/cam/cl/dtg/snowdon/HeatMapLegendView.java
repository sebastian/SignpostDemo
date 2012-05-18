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
 * HeatMapLegendView is the class for a HeatMap legend. Colours are specified
 * independently from those in the HeatMap, so care must be taken to make sure
 * they agree.
 * 
 * @author ajdm2
 *
 */
public class HeatMapLegendView extends View {
	protected Paint mTextPaint = new Paint();
	protected Paint mMinPaint = new Paint();
	protected Paint mMaxPaint = new Paint();
	protected Paint mNaNPaint = new Paint();
	protected Paint mRectPaint = new Paint();
	protected String mMaxText = "max";
	protected String mMinText = "min";
	protected String mNaNText = "no data";
	protected float mTopPadding = 10.0f;
	
	protected String customSchemaLocation;
	
	/**
	 * Creates a new HeatMapLegendView and sets layout parameters based on those
	 * defined in XML.
	 * 
	 * @param context
	 * @param attributes
	 */
	public HeatMapLegendView(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		//customSchemaLocation = getResources().getString(R.string.NS);
		customSchemaLocation = "http://schemas.android.com/apk/res/net.snowdon";
        
		mTextPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "heat_map_legend_text_colour", 0xFF000000));
		mRectPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "heat_map_legend_box_border_colour", 0xFF000000));
		mRectPaint.setStyle(Paint.Style.STROKE);
		mMinPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "heat_map_legend_min_colour", 0xFFeefbff));
		mMaxPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "heat_map_legend_max_colour", 0xFF006280));
		mNaNPaint.setColor(attributes.getAttributeIntValue(customSchemaLocation, "heat_map_legend_no_data_colour", 0xFFaeaeae));
		mTextPaint.setTextSize(attributes.getAttributeFloatValue(customSchemaLocation, "heat_map_legend_text_size", 20.0f));
		mTextPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "heat_map_legend_text_anti_alias", true));
		mRectPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "heat_map_legend_box_anti_alias", true));
		mMinPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "heat_map_legend_box_anti_alias", true));
		mMaxPaint.setAntiAlias(attributes.getAttributeBooleanValue(customSchemaLocation, "heat_map_legend_box_anti_alias", true));;
	}
	
	@Override
	public void onDraw(Canvas canvas) {		
		int quarterOfWidth = getWidth() / 4;
		float rectWidth = 20*getResources().getDisplayMetrics().density;
		
		canvas.drawText(mMinText, 1 * quarterOfWidth - mTextPaint.measureText(mMinText) / 2, mTopPadding + rectWidth + mTextPaint.getTextSize(), mTextPaint);
		canvas.drawRect(1 * quarterOfWidth - rectWidth / 2, mTopPadding, 1 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mMinPaint);
		canvas.drawRect(1 * quarterOfWidth - rectWidth / 2, mTopPadding, 1 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mRectPaint);
		
		canvas.drawText(mMaxText, 2 * quarterOfWidth - mTextPaint.measureText(mMaxText) / 2, mTopPadding + rectWidth + mTextPaint.getTextSize(), mTextPaint);
		canvas.drawRect(2 * quarterOfWidth - rectWidth / 2, mTopPadding, 2 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mMaxPaint);
		canvas.drawRect(2 * quarterOfWidth - rectWidth / 2, mTopPadding, 2 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mRectPaint);
		
		canvas.drawText(mNaNText, 3 * quarterOfWidth - mTextPaint.measureText(mNaNText) / 2, mTopPadding + rectWidth + mTextPaint.getTextSize(), mTextPaint);
		canvas.drawRect(3 * quarterOfWidth - rectWidth / 2, mTopPadding, 3 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mNaNPaint);
		canvas.drawRect(3 * quarterOfWidth - rectWidth / 2, mTopPadding, 3 * quarterOfWidth + rectWidth / 2, mTopPadding + rectWidth, mRectPaint);
	}

}
