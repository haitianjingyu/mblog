/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller.back;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.util.Validators;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.file.entity.BlogFile;
import me.qyh.blog.file.entity.BlogFile.BlogFileType;
import me.qyh.blog.file.service.FileService;
import me.qyh.blog.file.store.FileStore;
import me.qyh.blog.file.validator.BlogFileQueryParamValidator;
import me.qyh.blog.file.validator.BlogFileUploadValidator;
import me.qyh.blog.file.vo.BlogFileQueryParam;
import me.qyh.blog.file.vo.BlogFileUpload;
import me.qyh.blog.file.vo.FileStoreBean;
import me.qyh.blog.file.vo.UploadedFile;

@Controller
@RequestMapping("mgr/file")
public class FileMgrController extends BaseMgrController {

	@Autowired
	private FileService fileService;
	@Autowired
	private BlogFileQueryParamValidator blogFileParamValidator;
	@Autowired
	private BlogFileUploadValidator blogFileUploadValidator;

	@InitBinder(value = "blogFileQueryParam")
	protected void initBlogFileQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(blogFileParamValidator);
	}

	@InitBinder(value = "blogFileUpload")
	protected void initBlogUploadBinder(WebDataBinder binder) {
		binder.setValidator(blogFileUploadValidator);
	}

	@GetMapping("index")
	public String index(@Validated BlogFileQueryParam blogFileQueryParam, Model model) {
		try {
			model.addAttribute("result", fileService.queryBlogFiles(blogFileQueryParam));
			model.addAttribute("stores", fileService.allStorableStores());
		} catch (LogicException e) {
			model.addAttribute(Constants.ERROR, e.getLogicMessage());
		}
		return "mgr/file/index";
	}

	@GetMapping("stores")
	@ResponseBody
	public List<FileStoreBean> allServers() {
		List<FileStore> stores = fileService.allStorableStores();
		return stores.stream().map(FileStoreBean::new).collect(Collectors.toList());
	}

	@GetMapping("query")
	@ResponseBody
	public JsonResult query(@Validated BlogFileQueryParam blogFileQueryParam) throws LogicException {
		blogFileQueryParam.setQuerySubDir(false);
		blogFileQueryParam.setExtensions(new HashSet<>());
		return new JsonResult(true, fileService.queryBlogFiles(blogFileQueryParam));
	}

	@PostMapping("upload")
	@ResponseBody
	public JsonResult upload(@Validated BlogFileUpload blogFileUpload, BindingResult result) throws LogicException {
		if (result.hasErrors()) {
			List<ObjectError> errors = result.getAllErrors();
			for (ObjectError error : errors) {
				return new JsonResult(false,
						new Message(error.getCode(), error.getDefaultMessage(), error.getArguments()));
			}
		}
		List<UploadedFile> uploadedFiles = fileService.upload(blogFileUpload);
		return new JsonResult(true, uploadedFiles);
	}

	@PostMapping("delete")
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		fileService.delete(id);
		return new JsonResult(true, new Message("file.delete.success", "删除成功"));
	}

	@GetMapping("{id}/pro")
	@ResponseBody
	public JsonResult pro(@PathVariable("id") int id) throws LogicException {
		return new JsonResult(true, fileService.getBlogFileProperty(id));
	}
	
	@PostMapping({"{parent}/createFolder","createFolder"})
	@ResponseBody
	public JsonResult createFolder(@PathVariable Optional<Integer> parent,
			@RequestParam("path") String path) throws LogicException {
		if(Validators.isEmptyOrNull(path, true)){
			return new JsonResult(false, new Message("file.create.emptyPath", "文件夹地址不能为空"));
		}
		BlogFile blogFile = new BlogFile();
		parent.ifPresent(_id->{
			BlogFile _parent = new BlogFile();
			_parent.setId(_id);
			blogFile.setParent(_parent);
		});
		blogFile.setPath(path);
		blogFile.setType(BlogFileType.DIRECTORY);
		fileService.createFolder(blogFile);
		return new JsonResult(true, new Message("file.create.success", "创建成功"));
	}

	@PostMapping("copy")
	@ResponseBody
	public JsonResult copy(@RequestParam("sourceId") Integer sourceId, @RequestParam("folderPath") String folderPath)
			throws LogicException {
		if(Validators.isEmptyOrNull(folderPath, true)){
			return new JsonResult(false, new Message("file.copy.emptyFolderPath", "目标文件夹地址不能为空"));
		}
		fileService.copy(sourceId, folderPath);
		return new JsonResult(true, new Message("file.copy.success", "拷贝成功"));
	}

	@PostMapping("move")
	@ResponseBody
	public JsonResult move(@RequestParam("sourceId") Integer sourceId, @RequestParam("destPath") String destPath)
			throws LogicException {
		if(Validators.isEmptyOrNull(destPath, true)){
			return new JsonResult(false, new Message("file.move.emptyDestPath", "目标地址不能为空"));
		}
		fileService.move(sourceId, destPath);
		return new JsonResult(true, new Message("file.move.success", "移动成功"));
	}

	@PostMapping("rename")
	@ResponseBody
	public JsonResult rename(@RequestParam("sourceId") Integer sourceId, @RequestParam("newName") String newName)
			throws LogicException {
		if(Validators.isEmptyOrNull(newName, true)){
			return new JsonResult(false, new Message("file.rename.emptyNewName", "新文件名不能为空"));
		}
		fileService.rename(sourceId, newName);
		return new JsonResult(true, new Message("file.rename.success", "重命名	成功"));
	}
}
