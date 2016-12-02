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
package me.qyh.blog.comment;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.web.controller.BaseMgrController;

@RequestMapping("mgr/comment")
@Controller
public class CommentMgrController extends BaseMgrController {

	@Autowired
	private DftCommentService commentService;
	@Autowired
	private CommentConfigValidator commentConfigValidator;

	@InitBinder(value = "commentConfig")
	protected void initCommentConfigBinder(WebDataBinder binder) {
		binder.setValidator(commentConfigValidator);
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST, params = { "id" })
	@ResponseBody
	public JsonResult remove(@RequestParam("id") Integer id) throws LogicException {
		commentService.deleteComment(id);
		return new JsonResult(true, new Message("comment.delete.success", "删除成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST, params = { "ip", "articleId" })
	@ResponseBody
	public JsonResult remove(@RequestParam("ip") String ip, @Param("articleId") Integer articleId)
			throws LogicException {
		commentService.deleteComment(ip, articleId);
		return new JsonResult(true, new Message("comment.delete.success", "删除成功"));
	}

	@RequestMapping(value = "check", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult check(@RequestParam("id") Integer id) throws LogicException {
		commentService.checkComment(id);
		return new JsonResult(true, new Message("comment.check.success", "审核成功"));
	}

	@RequestMapping(value = "updateConfig", method = RequestMethod.GET)
	public String update(ModelMap model) {
		model.addAttribute("config", commentService.getCommentConfig());
		return "mgr/config/commentConfig";
	}

	@RequestMapping(value = "updateConfig", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated CommentConfig commentConfig) {
		commentService.updateCommentConfig(commentConfig);
		return new JsonResult(true, new Message("comment.config.update.success", "更新成功"));
	}

}
