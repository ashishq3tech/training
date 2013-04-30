/*   
   Copyright 2011-2012 The Cassandra Consortium (cassandra-fp7.eu)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eu.cassandra.training.behaviour;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import eu.cassandra.training.utils.Constants;
import eu.cassandra.training.utils.RNG;

public class Histogram implements ProbabilityDistribution
{
  protected int numberOfBins;

  protected double[] values;

  public Histogram (double[] values)
  {
    numberOfBins = values.length;
    this.values = values;
  }

  public Histogram (String filename) throws FileNotFoundException
  {
    // System.out.println(filename);
    Map<Integer, Double> histogram = new HashMap<Integer, Double>();
    File file = new File(filename);
    Scanner input = new Scanner(file);
    String nextLine = input.nextLine();
    String[] line = new String[2];

    // System.out.println(nextLine);

    while (input.hasNext()) {
      nextLine = input.nextLine();
      line = nextLine.split("-");
      // System.out.println(line[0] + " " + line[1]);

      line[1] = line[1].replace(",", ".");

      histogram.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));

    }

    numberOfBins = (Integer.parseInt(line[0])) + 1;
    values = new double[numberOfBins];

    for (int i = 0; i < numberOfBins; i++) {

      if (histogram.containsKey(i))
        values[i] = histogram.get(i);
      else
        values[i] = 0;
    }

    /*
     * double sum = 0;
     * 
     * for (int i = 0; i < numberOfBins; i++) sum += values[i];
     * 
     * System.out.println(sum);
     */
    input.close();
    file.deleteOnExit();
  }

  @Override
  public int getNumberOfParameters ()
  {
    return 1;
  }

  @Override
  public double getParameter (int index)
  {

    return numberOfBins;
  }

  @Override
  public void setParameter (int index, double value)
  {
  }

  @Override
  public void precompute (int startValue, int endValue, int nBins)
  {
  }

  public String getDescription ()
  {
    String description = "Histogram probability Frequency function";
    return description;
  }

  public double getProbability (int x)
  {
    if (x < 0)
      return 0;
    else
      return values[x];
  }

  public double getPrecomputedProbability (int x)
  {
    if (x < 0)
      return 0;
    else
      return values[x];
  }

  public int getPrecomputedBin ()
  {

    double dice = RNG.nextDouble();
    double sum = 0;
    for (int i = 0; i < numberOfBins; i++) {
      sum += values[i];

      if (dice < sum)
        return i;
    }
    return -1;
  }

  public void status ()
  {
    System.out.println("Histogram Distribution");
    System.out.println("Number of Beans: " + numberOfBins);
    System.out.println("Values:");

    for (int i = 0; i < values.length; i++) {
      System.out.println("Index: " + i + " Value: " + values[i]);
    }

  }

  public double[] getHistogram ()
  {

    return values;

  }

  public double getProbabilityGreaterEqual (int x)
  {
    double prob = 0;

    int start = (int) x;

    for (int i = start; i < values.length; i++)
      prob += values[i];

    return prob;
  }

  public double getProbabilityLess (int x)
  {
    return 1 - getProbabilityGreaterEqual(x);
  }

  public double[] movingAverage (int index, int window)
  {
    int side = 0;
    if (window % 2 == 1)
      side = (int) (window / 2);
    else {
      window++;
      side = (int) (window / 2) + 1;
    }

    double[] values = Arrays.copyOf(this.values, this.values.length);

    int startIndex = Math.max(index - side, 0);
    int endIndex = Math.min(index + side, Constants.MINUTES_PER_DAY);

    for (int i = startIndex; i < endIndex; i++) {
      double temp = 0;
      // System.out.print("Index:" + i + " Old Value: " + values[i]);

      for (int j = -side; j < side; j++)
        temp += values[i + j];

      values[i] = temp / window;
      // System.out.println(" New Value: " + values[i]);
    }

    double sum = 0;

    for (int i = 0; i < values.length; i++)
      sum += values[i];

    double diff = 1 - sum;
    double diffPortion = diff / window;

    System.out.println("Summary" + sum + " Difference: " + diff + " Portion: "
                       + diffPortion);

    for (int i = 0; i < window; i++)
      values[endIndex + i] += diffPortion;

    for (int i = 0; i < window; i++)
      values[startIndex - i] += diffPortion;

    return values;
  }

  @Override
  public void movePeak (int index, int interval)
  {

    values = movingAverage(index, interval);

  }

  @Override
  public double[] movePeakPreview (int index, int interval)
  {

    return movingAverage(index, interval);

  }

  public static void main (String[] args) throws FileNotFoundException
  {
    System.out.println("Testing Histogram.");

    RNG.init();

    Histogram g = new Histogram("Files/DurationHistogramOverall.csv");
    g.status();
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());

    g = new Histogram("Files/DailyTimesHistogramOverall.csv");
    g.status();
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());

    g = new Histogram("Files/StartTimeHistogramBinnedOverall.csv");
    g.status();
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());

  }

}
