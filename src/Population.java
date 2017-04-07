import msrcpsp.evaluation.BaseEvaluator;
import msrcpsp.evaluation.DurationEvaluator;
import msrcpsp.io.MSRCPSPIO;
import msrcpsp.scheduling.Resource;
import msrcpsp.scheduling.Schedule;
import msrcpsp.scheduling.Task;
import msrcpsp.scheduling.greedy.Greedy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Mateusz Alama
 */
public class Population
{
    public static final String fileDefinition = "assets/def_small/10_3_5_3.def";

    public ArrayList<Schedule> population;

    public Population()
    {
        population = new ArrayList<Schedule>();
    }

    public Population(int sizePopulation, boolean initialize)
    {
         population = new ArrayList<Schedule>();

        if(initialize)
            initialize(population, sizePopulation);
    }

    public ArrayList<Schedule> getPopulation() {
        return population;
    }

    public void addInd(Schedule individual)
    {
        population.add(individual);
    }

    public Schedule getInd(int index)
    {
        return population.get(index);
    }

    public int sizePop()
    {
        return population.size();
    }

    public Schedule analyseBest()
    {
        double best = Double.MAX_VALUE;
        Schedule bestInd = null;

        for(Schedule ind : population)
        {
            double duration = evaluateSchedule(ind);
            if(best > duration)
            {
                best = duration;
                bestInd = ind;
            }
        }

        return bestInd;
    }

    public void initialize(ArrayList<Schedule> population, int sizePopulation)
    {
        MSRCPSPIO reader = new MSRCPSPIO();

        for(int i=0; i <= sizePopulation; i++)
        {
            Schedule schedule = reader.readDefinition(fileDefinition);

            Task[] tasks = schedule.getTasks();

            int[] upperBounds = schedule.getUpperBounds(schedule.getTasks().length);
            Random random = new Random(System.currentTimeMillis());

            for (int e = 0; e < tasks.length; ++e)
            {
                List capableResources = schedule.getCapableResources(tasks[e]);
                schedule.assign(tasks[e], (Resource) capableResources.get((int) (random.nextDouble() * (double) upperBounds[e])));
            }

            population.add(schedule);
        }
    }

    public static double evaluateSchedule(Schedule schedule)
    {
        Greedy greedy = new Greedy(schedule.getSuccesors());
        greedy.buildTimestamps(schedule);
        BaseEvaluator evaluator = new DurationEvaluator(schedule);

        return evaluator.evaluate();
    }
}
