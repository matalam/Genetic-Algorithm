import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.Resource;
import msrcpsp.scheduling.Schedule;
import msrcpsp.scheduling.Task;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Mateusz Alama
 */
public class GeneticAlgorithm
{
    private static final String testingFile = "10_3_5_3.def";
    private static final String writingFile = "assets/solutions_large/200_20_55_9.sol";

    private static final double ODDS_MUTATION = 0.01;
    private static final double ODDS_CROSSOVER = 0.1;
    private static final int SIZE_POPULATION = 100;
    private static final int AMOUNT_GENERATION = 100;
    private static final int SIZE_TOURNAMENT = 4;

    private Schedule bestIndividual;
    private int bestGeneration;
    private List<Double> bestList;
    private List<Double> avgList;
    private List<Double> worstList;
    private MSRCPSPIO reader;

    public GeneticAlgorithm()
    {
        bestList = new ArrayList<Double>();
        avgList = new ArrayList<Double>();
        worstList = new ArrayList<Double>();
        reader = new MSRCPSPIO();

        bestGeneration = 0;
    }

    public Schedule selectionTournament(Population population)
    {
        Population tournament = new Population();

        for(int i = 0; i < SIZE_TOURNAMENT; i++)
        {
            int randomID = (int) (Math.random() * population.sizePop());
            tournament.addInd(population.getInd(randomID));
        }

        return tournament.analyseBest();
    }

    public Schedule[] crossover(Schedule indFirst, Schedule indSecond)
    {
        Task[] tasksParentFirst = indFirst.getTasks();
        Task[] tasksParentSecond = indSecond.getTasks();
        Task[] taskChildFirst = new Task[indFirst.getTasks().length];
        Task[] taskChildSecond = new Task[indFirst.getTasks().length];

        Schedule childFirst = new Schedule(indFirst.getTasks().length, indFirst.getResources().length);
        Schedule childSecond = new Schedule(indFirst.getTasks().length, indFirst.getResources().length);
        childSecond.setResources(indFirst.getResources());
        childSecond.setTasks(taskChildSecond);
        childFirst.setResources(indFirst.getResources());
        childFirst.setTasks(taskChildFirst);

        int pivot = ThreadLocalRandom.current().nextInt(1, tasksParentFirst.length - 1);

        for(int i = 0; i < indFirst.getTasks().length; i++)
        {
            if(i < pivot)
            {
                taskChildFirst[i] = tasksParentFirst[i];
                childFirst.assign(taskChildFirst[i], childFirst.getResources()[tasksParentFirst[i].getResourceId() - 1]);
                taskChildSecond[i] = tasksParentSecond[i];
                childSecond.assign(taskChildSecond[i], childSecond.getResources()[tasksParentSecond[i].getResourceId() - 1]);
            }
            else
            {
                taskChildFirst[i] = tasksParentSecond[i];
                childFirst.assign(taskChildFirst[i], childFirst.getResources()[tasksParentSecond[i].getResourceId() - 1]);
                taskChildSecond[i] = tasksParentFirst[i];
                childSecond.assign(taskChildSecond[i], childSecond.getResources()[tasksParentFirst[i].getResourceId() - 1]);
            }
        }

        Schedule[] children = {childFirst, childSecond};

        return children;
    }

    public Schedule mutation(Schedule ind)
    {
        Task[] tasks = ind.getTasks();
        List<Resource> capableResources;
        int[] upperBounds = ind.getUpperBounds(ind.getTasks().length);

        for (Task t : tasks)
        {
            if (Math.random() < ODDS_MUTATION)
            {
                capableResources = ind.getCapableResources(t);
                Resource newResources = capableResources.get((int) (Math.random() * upperBounds[t.getId()-1]));
                ind.assign(t, newResources);
            }
        }

        return new Schedule(ind);
    }

    public Schedule evaluatePopulation(int generation, Population population)
    {
        Schedule best = null;
        Schedule worst = null;
        double bestTime = 0.0f;
        double worstTime= 0.0f;
        double totalTime = 0.0f;

        for(int i=0; i < SIZE_POPULATION; ++i)
        {
            double duration = population.evaluateSchedule(population.getInd(i));

            if(best == null || bestTime > duration)
            {
                best = population.getInd(i);
                bestTime = duration;
            }

            if(worst == null || worstTime < duration)
            {
                worst = population.getInd(i);
                worstTime = duration;
            }

            totalTime = totalTime + duration;
        }

        addResult(TypeResult.WORST, worstTime);
        addResult(TypeResult.AVG, totalTime / SIZE_POPULATION);

        System.out.println("Pokolenie " + (generation + 1) + ": " + "Najlepszy: " + bestTime + ", Najgorszy: " + worstTime + ", Srednia: " +totalTime / SIZE_POPULATION);
        return best;
    }

    public void setBestIndividual(Schedule ind, int generation)
    {
        bestIndividual = ind;
        bestGeneration = generation;
    }

    public Schedule getBestIndividual()
    {
        return bestIndividual;
    }

    public int getBestGeneration()
    {
        return bestGeneration;
    }

    public static void printSchedule(Schedule schedule)
    {
        System.out.println("Ind [ ");
        Task[] a = schedule.getTasks();
        for(Task obj : a)
        {
            System.out.println(obj.getId() + " rID: " + obj.getResourceId() + " d: " + obj.getDuration() + "  s: " + obj.getStart() + " ; ");
        }
        System.out.println("]");
    }

    public void toFile()
    {
        try
        {
            reader.write(bestIndividual, writingFile);
        }
        catch (IOException e)
        {
            System.out.print("file exception");
        }
    }

    public double getWorstResult(List<Double> results)
    {
        double worstResult = results.get(0);
        for (Double res : results)
            if (res > worstResult)
                worstResult = res;

        return worstResult;
    }

    public double getAvgResult(List<Double> results)
    {
        double totalResult = 0;
        for (Double res : results)
            totalResult += res;

        return totalResult / results.size();
    }

    public void addResult(TypeResult type, Double result)
    {
        switch(type)
        {
            case BEST:
                bestList.add(result);
                break;
            case AVG:
                avgList.add(result);
                break;
            case WORST:
                worstList.add(result);
                break;
        }
    }

    public void printResults()
    {
        System.out.println("Najlepsze pokolenie: " + getBestGeneration() + " Najlepszy czas: " + Population.evaluateSchedule(getBestIndividual()));
        System.out.println("Sredni czas: " + getAvgResult(avgList));
        System.out.println("Najgorszy czas: " + getWorstResult(worstList));
    }

    public void showGraph()
    {
        double y_data[] = createDataPopulationGraph();

        XYChart chart = new XYChartBuilder().xAxisTitle("GENERATION").yAxisTitle("SCHEDULE DURATION").width(1500).height(800).theme(Styler.ChartTheme.Matlab).title(testingFile).build();
        chart.addSeries("BEST", y_data, createDataResultsGraph(TypeResult.BEST));
        chart.addSeries("AVG", y_data, createDataResultsGraph(TypeResult.AVG));
        chart.addSeries("WORST", y_data, createDataResultsGraph(TypeResult.WORST));

        new SwingWrapper(chart).displayChart();
    }

    public double[] createDataPopulationGraph()
    {
        double y_data[] = new double[AMOUNT_GENERATION];
        for (int i = 0; i < AMOUNT_GENERATION; ++i)
            y_data[i] = i;

        return y_data;
    }

    public double[] createDataResultsGraph(TypeResult type)
    {
        List<Double> results = new ArrayList<>();
        switch (type)
        {
            case BEST:
                results = bestList;
                break;
            case AVG:
                results = avgList;
                break;
            case WORST:
                results = worstList;
                break;
            default:
                break;
        }

        int i = 0;
        double x_data[] = new double[AMOUNT_GENERATION];
        for (Double result : results)
            x_data[i++] = result;

        return x_data;
    }

    public static void main(String[] args)
    {
        GeneticAlgorithm simulation = new GeneticAlgorithm();
        Population population = new Population(SIZE_POPULATION, true);
        simulation.setBestIndividual(simulation.evaluatePopulation(0, population), 1);
        simulation.addResult(TypeResult.BEST, population.evaluateSchedule(simulation.getBestIndividual()));

        for(int gen = 1; gen < AMOUNT_GENERATION; ++gen)
        {
            Population newPopulation = new Population();

            int currentPop = 0;
            while(currentPop < SIZE_POPULATION)
            {
                Schedule firstInd = new Schedule(simulation.selectionTournament(population));
                Schedule secondInd = new Schedule(simulation.selectionTournament(population));

                if(Math.random() < ODDS_CROSSOVER)
                {
                    Schedule[] children = simulation.crossover(firstInd, secondInd);
                    firstInd = new Schedule(children[0]);
                    secondInd = new Schedule(children[1]);
                }

                simulation.mutation(firstInd);
                simulation.mutation(secondInd);

                newPopulation.addInd(new Schedule(firstInd));
                currentPop++;

                if(currentPop < SIZE_POPULATION)
                {
                    newPopulation.addInd(new Schedule(secondInd));
                    currentPop++;
                }
            }

            Schedule best = simulation.evaluatePopulation(gen, newPopulation);

            population.getPopulation().clear();
            for(int i=0; i < SIZE_POPULATION; i++)
                population.addInd(new Schedule(newPopulation.getInd(i)));

            simulation.addResult(TypeResult.BEST, population.evaluateSchedule(best));

            if(population.evaluateSchedule(best) < population.evaluateSchedule(simulation.getBestIndividual()))
            {
                simulation.setBestIndividual(new Schedule(best), gen+1);
            }
        }

        simulation.toFile();
        simulation.printResults();
        simulation.showGraph();

    }
}
