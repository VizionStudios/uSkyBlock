package us.talabrek.ultimateskyblock.command.admin;

import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.async.JobManager;
import us.talabrek.ultimateskyblock.command.common.AbstractUSBCommand;
import us.talabrek.ultimateskyblock.command.common.CompositeUSBCommand;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static us.talabrek.ultimateskyblock.util.I18nUtil.tr;

/**
 * Command for reporting and controlling async jobs.
 */
public class JobsCommand extends CompositeUSBCommand {
    private final uSkyBlock plugin;

    public JobsCommand(uSkyBlock plugin) {
        super("jobs|j", "usb.admin.jobs", "controls async jobs");
        this.plugin = plugin;
        /*
        add(new AbstractUSBCommand("list|l", "usb.admin.jobs.list", "list all jobs") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                return false;
            }
        });
        */
        add(new AbstractUSBCommand("stats|s", "usb.admin.jobs.stats", "show statistics") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                StringBuilder sb = new StringBuilder();
                sb.append(tr("\u00a79Job Statistics") + "\n");
                sb.append(tr("\u00a77----------------") + "\n");
                Map<String, JobManager.Stats> stats = JobManager.getStats();
                List<String> jobs = new ArrayList<>(stats.keySet());
                Collections.sort(jobs);
                sb.append(String.format("\u00a77%-6s %-8s %-8s %-8s %-8s %-8s %-20s\n",
                        tr("#"), tr("ms/job"), tr("ms/tick"), tr("ticks"), tr("act"), tr("time"), tr("name")));
                for (String jobName : jobs) {
                    JobManager.Stats stat = stats.get(jobName);
                    sb.append(String.format("\u00a77%6d %8s %8s %8d \u00a7c%8d \u00a77%8s \u00a79%-20s \n", stat.getJobs(),
                            TimeUtil.millisAsShort(Math.round(stat.getAvgMsActivePerJob())),
                            TimeUtil.millisAsShort(Math.round(stat.getAvgMsActivePerTick())),
                            stat.getTicks(),
                            stat.getRunningJobs(),
                            TimeUtil.millisAsShort(Math.round(stat.getAvgMsElapsedPerJob())),
                            tr(jobName)
                    ));
                }
                sender.sendMessage(sb.toString().split("\n"));
                return true;
            }
        });
        /*
        add(new AbstractUSBCommand("report|r", "usb.admin.jobs.report", "dump report to file") {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                return false;
            }
        });
        */
    }
}
