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

/**
 * Provides utility methods for the binning of data.
 * 
 * @author ajdm2
 *
 */
public class DataUtilities {
	/**
	 * Bins a two-dimensional float array into a smaller two-dimensional float
	 * array.
	 * 
	 * @param data Two-dimensional float array with x data first.
	 * @param binWidth X range to use for binning.
	 * @return Two-dimensional float array containing binned data.
	 */
	public static float[][] binFloatFloatArray(float[][] data, float binWidth, float minX, float maxX) {
		if (binWidth == 0) {
			return data;
		} else {
			float[] xData = data[0];
			float[] yData = data[1];

			int numOfBins = (int) (1 + (maxX - minX) / binWidth);
			float[][] binnedData = new float[2][numOfBins];
			
			// Find the start and end element indices
			int startIndex = 0;
			int endIndex = 0;
			for (int i = 0; i < xData.length; i++) {
				if (xData[i] >= minX) {
					startIndex = i;
					break;
				}
			}
			for (int i = 0; i < xData.length; i++) {
				if (xData[i] > maxX) {
					endIndex = i - 1;
					break;
				}
			}
			if (endIndex == 0) {
				endIndex = xData.length - 1;
			}

			for (int i = startIndex; i <= endIndex; i++) {
				int binNum = (int) ((xData[i] - minX) / binWidth);
				binnedData[0][binNum] = xData[i];
				if (yData[i] > binnedData[1][binNum]) binnedData[1][binNum] = yData[i];
			}

			ArrayList<Float> filteredBinnedX = new ArrayList<Float>();
			ArrayList<Float> filteredBinnedY = new ArrayList<Float>();

			for (int i = 0; i < binnedData[0].length; i++) {
				if (binnedData[0][i] != 0) {
					filteredBinnedX.add(binnedData[0][i]);
					filteredBinnedY.add(binnedData[1][i]);
				}
			}

			float[][] filteredBinnedData = new float[2][filteredBinnedX.size()];

			for (int i = 0; i < filteredBinnedX.size(); i++) {
				filteredBinnedData[0][i] = filteredBinnedX.get(i);
				filteredBinnedData[1][i] = filteredBinnedY.get(i);
			}

			return filteredBinnedData;
		}
	}
	
	public static float[][] convertToFreqDensity(float[][] data) {
		float[][] freqDensityData = new float[data.length][3];
		for (int i = 0; i < data.length; i++) {
			float startX = data[i][0];
			float endX = data[i][1];
			float y = data[i][2];
			
			float xRange = endX - startX;
			float freqDensity = y / xRange;
			
			freqDensityData[i][0] = startX;
			freqDensityData[i][1] = endX;
			freqDensityData[i][2] = freqDensity;
		}
		
		return freqDensityData;
	}
	
	public static int[] countFrequencies(float[] data, float[] searchValues) {
		int[] counts = new int[searchValues.length];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < searchValues.length; j++) {
				if (data[i] == searchValues[j]) {
					counts[j]++;
				}
			}
		}
		
		return counts;
	}
	
	public static int[] countFrequenciesOfRanges(float[] data, float[][] searchRanges) {
		int counts[] = new int[searchRanges.length];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < searchRanges.length; j++) {
				if (data[i] > searchRanges[j][0] && data[i] <= searchRanges[j][1]) {
					counts[j]++;
				}
			}
		}
		
		return counts;
	}
}
