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
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import eu.cassandra.training.response.PeakFinder;
import eu.cassandra.training.utils.Constants;
import eu.cassandra.training.utils.RNG;
import eu.cassandra.training.utils.Utils;

/**
 * @author Antonios Chrysopoulos
 * @version prelim
 * @since 2012-26-06
 */
public class GaussianMixtureModels implements ProbabilityDistribution
{
  protected double[] pi;
  protected Gaussian[] gaussians;

  // For precomputation
  protected boolean precomputed;
  protected int numberOfBins;
  protected double precomputeFrom;
  protected double precomputeTo;
  protected double[] histogram;

  /**
   * Constructor. Sets the parameters of the standard normal distribution,
   * with mean 0 and standard deviation 1.
   */
  public GaussianMixtureModels (int n)
  {
    pi = new double[n];
    for (int i = 0; i < n; i++) {
      pi[i] = (1.0 / n);
      gaussians[i] = new Gaussian();
    }
    precomputed = false;
  }

  /**
   * @param mu
   *          Mean value of the Gaussian distribution.
   * @param s
   *          Standard deviation of the Gaussian distribution.
   */
  public GaussianMixtureModels (int n, double[] pi, double[] mu, double[] s)
  {
    gaussians = new Gaussian[n];
    for (int i = 0; i < n; i++) {
      this.pi = pi;
      gaussians[i] = new Gaussian(mu[i], s[i]);
    }
    precomputed = false;
  }

  public GaussianMixtureModels (String filename) throws FileNotFoundException
  {

    File file = new File(filename);
    Scanner input = new Scanner(file);
    String nextLine = input.nextLine();

    String[] temp = nextLine.split(":");
    int maxValue = Integer.parseInt(temp[1]);

    // Read the number of mixtures
    nextLine = input.nextLine();
    int n = Integer.parseInt(nextLine);
    String[] line = new String[n];

    // Read the percentage of presence
    nextLine = input.nextLine();
    line = nextLine.split("-");
    pi = new double[n];
    for (int i = 0; i < n; i++) {

      pi[i] = Double.parseDouble(line[i].replace(",", "."));

    }

    // Read the means
    nextLine = input.nextLine();
    line = nextLine.split("-");
    double[] mu = new double[n];
    for (int i = 0; i < n; i++) {

      mu[i] = Double.parseDouble(line[i].replace(",", "."));

    }

    // Read the sigmas
    nextLine = input.nextLine();
    line = nextLine.split("-");
    double[] s = new double[n];
    for (int i = 0; i < n; i++) {

      s[i] = Double.parseDouble(line[i].replace(",", "."));

    }

    gaussians = new Gaussian[n];
    for (int i = 0; i < n; i++) {
      gaussians[i] = new Gaussian(mu[i], s[i]);
    }

    precompute(0, maxValue, maxValue);

    // status();

    input.close();

  }

  public String getDescription ()
  {
    String description = "Gaussian Mixture Models probability density function";
    return description;
  }

  public int getNumberOfParameters ()
  {
    return 3 * pi.length;
  }

  public double[] getParameters (int index)
  {
    double[] temp = new double[3];

    temp[0] = gaussians[index].getParameter(0);
    temp[1] = gaussians[index].getParameter(1);
    temp[2] = pi[index];

    return temp;
  }

  public void setParameters (int index, double[] values)
  {

    gaussians[index].setParameter(0, values[0]);
    gaussians[index].setParameter(1, values[1]);
    pi[index] = values[2];

  }

  public void precompute (int startValue, int endValue, int nBins)
  {
    if (startValue >= endValue) {
      // TODO Throw an exception or whatever.
      return;
    }
    precomputeFrom = startValue;
    precomputeTo = endValue;
    numberOfBins = nBins;
    histogram = new double[nBins];

    for (int i = 0; i < gaussians.length; i++) {
      gaussians[i].precompute(startValue, endValue, nBins);
    }

    for (int i = 0; i < nBins; i++) {

      for (int j = 0; j < gaussians.length; j++) {
        histogram[i] += pi[j] * gaussians[j].getPrecomputedProbability(i);
      }

    }

    precomputed = true;
  }

  public double getProbability (int x)
  {
    double sum = 0;
    for (int j = 0; j < pi.length; j++) {
      sum += pi[j] * gaussians[j].getProbability(x);
    }
    return sum;
  }

  public double getCumulativeProbability (int z)
  {
    double sum = 0;
    for (int j = 0; j < pi.length; j++) {
      sum +=
        pi[j]
                * Gaussian.bigPhi(z, gaussians[j].getParameter(0),
                                  gaussians[j].getParameter(1));
    }
    return sum;
  }

  public double getParameter (int index)
  {
    return 0;
  }

  public void setParameter (int index, double value)
  {
  }

  public double getPrecomputedProbability (int x)
  {
    if (!precomputed) {
      return -1;
    }
    double div = (precomputeTo - precomputeFrom) / (double) numberOfBins;
    int bin = (int) Math.floor((x - precomputeFrom) / div);
    if (bin == numberOfBins) {
      bin--;
    }
    return histogram[bin];
  }

  public int getPrecomputedBin ()
  {
    if (!precomputed) {
      return -1;
    }
    // double div = (precomputeTo - precomputeFrom) / (double) numberOfBins;
    double dice = RNG.nextDouble();
    double sum = 0;
    for (int i = 0; i < numberOfBins; i++) {
      sum += histogram[i];
      // if(dice < sum) return (int)(precomputeFrom + i * div);
      if (dice < sum)
        return i;
    }
    return -1;
  }

  public double[] getHistogram ()
  {

    return histogram;

  }

  public void status ()
  {

    System.out.print("Gaussian Mixture with");
    System.out.println(" Number of Mixtures:" + pi.length);
    for (int i = 0; i < pi.length; i++) {
      System.out.print("Mixture " + i);
      System.out.print(" Mean: " + gaussians[i].getParameter(0));
      System.out.print(" Sigma: " + gaussians[i].getParameter(1));
      System.out.print(" Weight: " + pi[i]);
      System.out.println();
    }
    System.out.println("Precomputed: " + precomputed);
    if (precomputed) {
      System.out.print("Number of Beans: " + numberOfBins);
      System.out.print(" Starting Point: " + precomputeFrom);
      System.out.println(" Ending Point: " + precomputeTo);
    }
    System.out.println(Arrays.toString(histogram));
  }

  public static void main (String[] args) throws IOException
  {
    System.out.println("Testing Mixture Creation.");

    GaussianMixtureModels g;
    RNG.init();
    /*
     * double[] pi = { 0.3, 0.5, 0.2 };
     * 
     * double[] mean = { 100, 200, 300 };
     * 
     * double[] std = { 10, 20, 30 };
     * 
     * g = new GaussianMixtureModels(3, pi, mean, std);
     */
    g = new GaussianMixtureModels("Files/newGMM.csv");

    double[] temp = g.getHistogram();
    double sum = 0;

    for (int i = 0; i < Constants.MINUTES_PER_DAY; i++)
      sum += temp[i];

    System.out.println("Summary:" + sum);
    PeakFinder pf = new PeakFinder(temp);

    pf.findLocalIntervalMaxima();
    pf.showListInterval();
    System.out.println(pf.findGlobalIntervalMaximum().toString());
    System.out.println();

    int peakIndex = pf.findGlobalIntervalMaximum().getIndexMinute();

    // g.movePeak(peakIndex * Constants.MINUTES_PER_BIN, 30);

    g.precompute(0, 1440, 1440);

    double[] temp2 = g.getHistogram();

    System.out.println(temp.length + " " + temp2.length);

  }

  public double getProbabilityGreaterEqual (int x)
  {
    double prob = 0;

    int start = (int) x;

    for (int i = start; i < histogram.length; i++)
      prob += histogram[i];

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

    double[] histogram = Arrays.copyOf(this.histogram, this.histogram.length);

    int startIndex = Math.max(index - side, 0);
    int endIndex = Math.min(index + side, Constants.MINUTES_PER_DAY);

    for (int i = startIndex; i < endIndex; i++) {
      double temp = 0;
      // System.out.print("Index:" + i + " Old Value: " + values[i]);

      for (int j = -side; j < side; j++)
        temp += histogram[i + j];

      histogram[i] = temp / window;
      // System.out.println("New Value: " + values[i]);
    }

    double sum = 0;

    for (int i = 0; i < histogram.length; i++)
      sum += histogram[i];

    double diff = 1 - sum;
    double diffPortion = diff / window;

    System.out.println("Summary: " + sum + " Difference: " + diff
                       + " Portion: " + diffPortion);

    for (int i = 0; i < window; i++)
      histogram[endIndex + i] += diffPortion;

    for (int i = 0; i < window; i++)
      histogram[startIndex - i] += diffPortion;

    return histogram;
  }

  // @Override
  // public void shifting (int shiftingCase, double[] basicScheme,
  // double[] newScheme)
  // {
  //
  // histogram = movingAverage(index, interval);
  //
  // }
  //
  // @Override
  // public double[] shiftingPreview (int shiftingCase, double[] basicScheme,
  // double[] newScheme)
  // {
  //
  // return movingAverage(index, interval);
  //
  // }

  @Override
  public void shifting (int shiftingCase, double[] basicScheme,
                        double[] newScheme)
  {

    if (shiftingCase == 0) {

      histogram = shiftingBest(newScheme);
    }
    else if (shiftingCase == 1) {

      histogram = shiftingNormal(basicScheme, newScheme);
    }
    else if (shiftingCase == 2) {

      histogram = shiftingWorst(basicScheme, newScheme);
    }
    else {
      System.out.println("ERROR in shifting function");
    }

  }

  @Override
  public double[] shiftingPreview (int shiftingCase, double[] basicScheme,
                                   double[] newScheme)
  {

    double[] result = new double[Constants.MINUTES_PER_DAY];

    if (shiftingCase == 0) {

      result = shiftingBest(newScheme);
    }
    else if (shiftingCase == 1) {

      result = shiftingNormal(basicScheme, newScheme);
    }
    else if (shiftingCase == 2) {

      result = shiftingWorst(basicScheme, newScheme);
    }
    else {
      System.out.println("ERROR in shifting function");
    }

    result = Utils.aggregateStartTimeDistribution(result);

    return result;

  }

  @Override
  public double[] shiftingBest (double[] newScheme)
  {
    double[] result = new double[Constants.MINUTES_PER_DAY];

    double sum = 0;

    for (int i = 0; i < newScheme.length; i++) {
      result[i] = histogram[i] / newScheme[i];
      sum += result[i];
    }

    for (int i = 0; i < result.length; i++)
      result[i] /= sum;

    return result;

  }

  @Override
  public double[] shiftingNormal (double[] basicScheme, double[] newScheme)
  {
    double[] result = new double[Constants.MINUTES_PER_DAY];

    return result;

  }

  @Override
  public double[] shiftingWorst (double[] basicScheme, double[] newScheme)
  {
    double[] result = new double[Constants.MINUTES_PER_DAY];

    return result;
  }

  public void movePeak2 (int index, int interval)
  {

    double max = Double.NEGATIVE_INFINITY;
    int maxIndex = -1;
    double temp = 0;
    double distance = 0;
    for (int i = 0; i < gaussians.length; i++) {

      temp +=
        Math.abs(pi[i] * (gaussians[i].sigma / (gaussians[i].mean - index)));

    }

    for (int i = 0; i < gaussians.length; i++) {

      double aDistance =
        (pi[i] * (gaussians[i].sigma / (gaussians[i].mean - index))) / temp;

      distance = interval * aDistance;

      if (max < aDistance) {
        max = aDistance;
        maxIndex = i;
      }

      // System.out.println("Index:" + i + " Distance: " + distance);

      gaussians[i] =
        new Gaussian(gaussians[i].mean + distance, gaussians[i].sigma
                                                   + Math.abs(distance));

    }

    System.out.println("Max Index: " + maxIndex + " Max Distance: " + max);

    double temp2 = 0;
    for (int i = 0; i < gaussians.length; i++)
      temp2 += pi[i];

    System.out.println("Summary Before: " + temp2);
    // ============================================================

    double diff = (interval / 100) * pi[maxIndex];
    double portion = temp2 - pi[maxIndex];
    pi[maxIndex] -= diff;

    for (int i = 0; i < gaussians.length; i++)
      if (i != maxIndex)
        pi[i] = pi[i] * (1 + diff / portion);

    temp2 = 0;
    for (int i = 0; i < gaussians.length; i++)
      temp2 += pi[i];

    System.out.println("Max Index: " + maxIndex + " pi: " + pi[maxIndex]);

    System.out.println("Summary After: " + temp2);

  }

  public void movePeakAlt (int index, int interval)
  {
    double temp = 0;
    double distance = 0;

    for (int i = 0; i < gaussians.length; i++) {

      temp +=
        Math.abs(pi[i] * (gaussians[i].sigma / (gaussians[i].mean - index)));

    }

    for (int i = 0; i < gaussians.length; i++) {

      distance =
        (interval * pi[i] * (gaussians[i].sigma / (gaussians[i].mean - index)))
                / temp;

      // System.out.println("Index:" + i + " Distance: " + distance);

      gaussians[i] =
        new Gaussian(gaussians[i].mean + distance, gaussians[i].sigma);

    }

  }
}
