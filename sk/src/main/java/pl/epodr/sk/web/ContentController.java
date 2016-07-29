package pl.epodr.sk.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/content")
public class ContentController {

	@RequestMapping("/{collectionId}/{collectionVersion}/{variant}/{filename}/module.{extension}")
	public void handleNewModuleUrl(@PathVariable String collectionId, @PathVariable long collectionVersion,
			@PathVariable String variant, @PathVariable String moduleId, @PathVariable String extension,
			HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String oldPath = String.format("/content/%s/%d/%s/%s.%s", collectionId, collectionVersion, variant, moduleId,
				extension);
		request.getRequestDispatcher(oldPath).forward(request, response);
	}

}
