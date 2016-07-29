package pl.epodr.sk.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import pl.epodr.sk.Configuration;
import pl.epodr.sk.QueueManager;
import pl.epodr.sk.common.CurrentSkModeHolder;
import pl.epodr.sk.files.DownloadManager;
import pl.epodr.sk.files.HttpClient;
import pl.epodr.sk.task.Task;
import pl.epodr.sk.web.RtHealthChecker.HealthCheckException;

@Controller
@RequestMapping("/")
public class StatusController {

	@Autowired
	private QueueManager queueManager;

	@Autowired
	private DownloadManager downloadManager;

	@Autowired
	private CurrentSkModeHolder skMode;

	@Autowired
	private Configuration configuration;

	@Autowired
	private HttpClient httpClient;

	@ModelAttribute("skMode")
	public String getSkMode() {
		return skMode.getMode();
	}

	@RequestMapping({ "/health-check", "/content-health-check" })
	@ResponseBody
	public String checkHealth(@RequestParam(required = false) String expect, HttpServletResponse response)
			throws IOException {
		if (skMode.getMode().equals(expect) || (expect == null && skMode.isProduction())) {
			return "OK";
		}

		response.sendError(412, "Invalid SK instance expected, it's " + skMode.getMode() + " here");
		return null;
	}

	@RequestMapping("/RT-health-check")
	@ResponseBody
	public String checkRtHealth(HttpServletResponse response) throws MalformedURLException, IOException {
		String rtHealthCheckUrl = downloadManager.getRtHealthCheckUrl();
		String womiUrl = downloadManager.getWomiEmitUrl(downloadManager.getRtHealthCheckWomiId(), "classic");
		String collxmlUrl = downloadManager.getColXmlUrl(downloadManager.getRtHealthCheckCollection());

		try {
			RtHealthChecker.checkUrl(rtHealthCheckUrl);
			RtHealthChecker.checkUrl(womiUrl);
			RtHealthChecker.checkUrl(collxmlUrl);
		} catch (HealthCheckException e) {
			response.setStatus(500);
			return e.getMessage();
		}

		return "OK";
	}

	@RequestMapping("/status")
	public ModelAndView getStatus(HttpServletResponse response) throws InterruptedException {
		List<Task> tasksInRun = queueManager.getRunningTasks();
		List<Task> tasksInQueue = queueManager.getQueue();

		ModelAndView mav = new ModelAndView("status");
		mav.addObject("tasksInRun", tasksInRun);
		mav.addObject("tasksInQueue", tasksInQueue);
		mav.addObject("numberOfTasks", tasksInRun.size() + tasksInQueue.size());

		return mav;
	}

}
