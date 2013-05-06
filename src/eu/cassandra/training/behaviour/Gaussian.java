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
import java.util.Scanner;

import eu.cassandra.training.utils.Constants;
import eu.cassandra.training.utils.RNG;
import eu.cassandra.training.utils.Utils;

/**
 * @author Christos Diou <diou remove this at iti dot gr>
 * @version prelim
 * @since 2012-22-01
 */
public class Gaussian implements ProbabilityDistribution
{
  // private static double small_number = 1.0E7;

  protected double mean;
  protected double sigma;

  // For precomputation
  protected boolean precomputed;
  protected int numberOfBins;
  protected double precomputeFrom;
  protected double precomputeTo;
  protected double[] histogram;

  // return phi(x) = standard Gaussian pdf
  private static double phi (double x)
  {
    return Math.exp(-(x * x) / 2) / Math.sqrt(2 * Math.PI);
  }

  // return phi(x, mu, s) = Gaussian pdf with mean mu and stddev s
  private static double phi (int x, double mu, double s)
  {
    return phi((x - mu) / s) / s;
  }

  // return Phi(z) = standard Gaussian cdf using Taylor approximation
  private static double bigPhi (double z)
  {
    if (z < -8.0) {
      return 0.0;
    }
    if (z > 8.0) {
      return 1.0;
    }

    double sum = 0.0;
    double term = z;
    for (int i = 3; Math.abs(term) > 1e-5; i += 2) {
      sum += term;
      term *= (z * z) / i;
    }
    return 0.5 + sum * phi(z);
  }

  // return Phi(z, mu, s) = Gaussian cdf with mean mu and stddev s
  protected static double bigPhi (double z, double mu, double s)
  {
    return bigPhi((z - mu) / s);
  }

  /**
   * Constructor. Sets the parameters of the standard normal distribution,
   * with mean 0 and standard deviation 1.
   */
  public Gaussian ()
  {
    mean = 0.0;
    sigma = 1.0;
    precomputed = false;
  }

  /**
   * @param mu
   *          Mean value of the Gaussian distribution.
   * @param s
   *          Standard deviation of the Gaussian distribution.
   */
  public Gaussian (double mu, double s)
  {
    mean = mu;
    sigma = s;
    precomputed = false;
  }

  public Gaussian (String filename) throws FileNotFoundException
  {

    File file = new File(filename);
    Scanner input = new Scanner(file);
    String nextLine = input.nextLine();

    String[] temp = nextLine.split(":");
    int maxValue = Integer.parseInt(temp[1]);

    nextLine = input.nextLine();

    mean = Double.parseDouble(nextLine.replace(",", "."));

    nextLine = input.nextLine();

    sigma = Double.parseDouble(nextLine.replace(",", "."));

    precompute(0, maxValue, maxValue);

    input.close();

  }

  public String getDescription ()
  {
    String description = "Gaussian probability density function";
    return description;
  }

  public int getNumberOfParameters ()
  {
    return 2;
  }

  public double getParameter (int index)
  {
    switch (index) {
    case 0:
      return mean;
    case 1:
      return sigma;
    default:
      return 0.0;
    }

  }

  public void setParameter (int index, double value)
  {
    switch (index) {
    case 0:
      mean = value;
      break;
    case 1:
      sigma = value;
      break;
    default:
      return;
    }
  }

  public void precompute (int startValue, int endValue, int nBins)
  {
    if ((startValue >= endValue) || (nBins == 0)) {
      System.out.println("Start Value > End Value or Number of Bins = 0");
      return;
    }
    precomputeFrom = startValue;
    precomputeTo = endValue;
    numberOfBins = nBins;

    double div = (endValue - startValue) / (double) nBins;
    histogram = new double[nBins];

    double residual =
      bigPhi(startValue, mean, sigma) + 1 - bigPhi(endValue, mean, sigma);
    residual /= nBins;
    for (int i = 0; i < nBins; i++) {
      // double x = startValue + i * div - small_number;
      double x = startValue + i * div;
      histogram[i] =
        bigPhi(x + div / 2.0, mean, sigma) - bigPhi(x - div / 2.0, mean, sigma);
      histogram[i] += residual;
    }
    precomputed = true;
  }

  public double getProbability (int x)
  {
    return phi(x, mean, sigma);
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

  public void status ()
  {
    System.out.print("Normal Distribution with");
    System.out.print(" Mean: " + getParameter(0));
    System.out.println(" Sigma: " + getParameter(1));
    System.out.println("Precomputed: " + precomputed);
    if (precomputed) {
      System.out.print("Number of Bins: " + numberOfBins);
      System.out.print(" Starting Point: " + precomputeFrom);
      System.out.println(" Ending Point: " + precomputeTo);
    }
    System.out.println(Arrays.toString(histogram));

  }

  public double[] getHistogram ()
  {

    return histogram;

  }

  public static void main (String[] args) throws FileNotFoundException
  {
    System.out.println("Testing num of time per day.");
    Gaussian g = new Gaussian(1, 0.00001);
    g.precompute(0, 1440, 1440);
    g.status();
    double sum = 0;
    for (int i = 0; i <= 3; i++) {
      sum += g.getPrecomputedProbability(i);
      System.out.println(g.getPrecomputedProbability(i));
    }
    System.out.println("Sum = " + sum);
    RNG.init();
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println("Testing start time.");
    g = new Gaussian(620, 200);
    g.precompute(0, 1440, 1440);
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println("Testing duration.");
    g = new Gaussian("Files/1GMMStartTime.csv");
    g.precompute(1, 1440, 1440);
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());
    System.out.println(g.getPrecomputedBin());

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

}
