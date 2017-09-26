package me.qyh.blog.web.template;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import me.qyh.blog.comment.dao.CommentDao;
import me.qyh.blog.comment.entity.Comment;
import me.qyh.blog.comment.entity.CommentModule;
import me.qyh.blog.comment.module.CommentModuleHandler;
import me.qyh.blog.comment.service.CommentService;
import me.qyh.blog.comment.vo.ModuleCommentCount;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.event.EventType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.web.template.dao.PageCommentDao;
import me.qyh.blog.web.template.dao.PageDao;
import me.qyh.blog.web.template.entity.Page;
import me.qyh.blog.web.template.event.PageEvent;

public class PageCommentModuleHandler extends CommentModuleHandler implements InitializingBean {

	private static final String MODULE_NAME = "userpage";

	@Autowired
	private CommentDao commentDao;
	@Autowired
	private PageDao pageDao;
	@Autowired
	private PageCommentDao pageCommentDao;

	@Autowired
	private CommentService commentService;

	public PageCommentModuleHandler() {
		super(MODULE_NAME);
	}

	@Override
	public void doValidateBeforeInsert(Integer id) throws LogicException {
		Page page = pageDao.selectById(id);
		if (page == null) {
			throw new LogicException("page.user.notExists", "页面不存在");
		}
		if (!page.getAllowComment() && !Environment.isLogin()) {
			throw new LogicException("page.notAllowComment", "页面不允许评论");
		}
	}

	@Override
	public boolean doValidateBeforeQuery(Integer id) {
		Page page = pageDao.selectById(id);
		return page != null && Environment.match(page.getSpace());
	}

	@Override
	public Map<Integer, Integer> queryCommentNums(Collection<Integer> ids) {
		return Collections.emptyMap();
	}

	@Override
	public OptionalInt queryCommentNum(Integer id) {
		ModuleCommentCount count = commentDao.selectCommentCount(new CommentModule(MODULE_NAME, id));
		return count == null ? OptionalInt.empty() : OptionalInt.of(count.getComments());
	}


	@Override
	public Map<Integer, Object> getReferences(Collection<Integer> ids) {
		List<Page> pages = pageDao.selectSimpleByIds(ids);
		return pages.stream().collect(Collectors.toMap(Page::getId, p -> p));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.commentService.addCommentModuleHandler(this);
	}

	@EventListener
	public void handlePageEvent(PageEvent event) {
		if (event.getType().equals(EventType.DELETE)) {
			commentDao.deleteByModule(new CommentModule(MODULE_NAME, event.getPage().getId()));
		}
	}

	@Override
	public List<Comment> queryLastComments(Space space, int limit, boolean queryPrivate, boolean queryAdmin) {
		return pageCommentDao.selectLastComments(space, limit, queryPrivate, queryAdmin);
	}

	@Override
	public int queryCommentNum(Space space, boolean queryPrivate) {
		return pageCommentDao.selectTotalCommentCount(space, queryPrivate);
	}

}
