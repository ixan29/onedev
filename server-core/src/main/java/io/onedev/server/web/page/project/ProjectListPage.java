package io.onedev.server.web.page.project;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.User;
import io.onedev.server.model.support.NamedProjectQuery;
import io.onedev.server.model.support.NamedQuery;
import io.onedev.server.model.support.QuerySetting;
import io.onedev.server.model.support.administration.GlobalProjectSetting;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.project.list.ProjectListPanel;
import io.onedev.server.web.component.savedquery.NamedQueriesBean;
import io.onedev.server.web.component.savedquery.SaveQueryPanel;
import io.onedev.server.web.component.savedquery.SavedQueriesPanel;
import io.onedev.server.web.page.layout.LayoutPage;
import io.onedev.server.web.util.NamedProjectQueriesBean;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public class ProjectListPage extends LayoutPage {

	private static final String PARAM_PAGE = "page";
	
	private static final String PARAM_QUERY = "query";
	
	private static final String PARAM_EXPECTED_COUNT = "expectedCount";
	
	private final String query;
	
	private final int expectedCount;

	public ProjectListPage(PageParameters params) {
		super(params);
		query = getPageParameters().get(PARAM_QUERY).toOptionalString();
		expectedCount = params.get(PARAM_EXPECTED_COUNT).toInt(0);
	}

	private static GlobalProjectSetting getProjectSetting() {
		return OneDev.getInstance(SettingManager.class).getProjectSetting();		
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		SavedQueriesPanel<NamedProjectQuery> savedQueries;
		add(savedQueries = new SavedQueriesPanel<NamedProjectQuery>("side") {

			@Override
			protected NamedQueriesBean<NamedProjectQuery> newNamedQueriesBean() {
				return new NamedProjectQueriesBean();
			}

			@Override
			protected Link<Void> newQueryLink(String componentId, NamedProjectQuery namedQuery) {
				return new BookmarkablePageLink<Void>(componentId, ProjectListPage.class, 
						ProjectListPage.paramsOf(namedQuery.getQuery(), 0, 0));
			}

			@Override
			protected QuerySetting<NamedProjectQuery> getQuerySetting() {
				if (getLoginUser() != null)
					return getLoginUser().getProjectQuerySetting();
				else
					return null;
			}

			@Override
			protected ArrayList<NamedProjectQuery> getQueries() {
				return (ArrayList<NamedProjectQuery>) getProjectSetting().getNamedQueries();
			}

			@Override
			protected void onSaveQuerySetting(QuerySetting<NamedProjectQuery> querySetting) {
				OneDev.getInstance(UserManager.class).save(getLoginUser());
			}

			@Override
			protected void onSaveQueries(ArrayList<NamedProjectQuery> namedQueries) {
				getProjectSetting().setNamedQueries(namedQueries);
				OneDev.getInstance(SettingManager.class).saveProjectSetting(getProjectSetting());
			}

		});
		
		PagingHistorySupport pagingHistorySupport = new PagingHistorySupport() {

			@Override
			public PageParameters newPageParameters(int currentPage) {
				PageParameters params = paramsOf(query, 0, 0);
				params.add(PARAM_PAGE, currentPage+1);
				return params;
			}
			
			@Override
			public int getCurrentPage() {
				return getPageParameters().get(PARAM_PAGE).toInt(1)-1;
			}
			
		};
		
		add(new ProjectListPanel("main", query, expectedCount) {

			@Override
			protected PagingHistorySupport getPagingHistorySupport() {
				return pagingHistorySupport;
			}

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target, String query) {
				setResponsePage(ProjectListPage.class, paramsOf(query, 0, 0));
			}

			@Override
			protected QuerySaveSupport getQuerySaveSupport() {
				return new QuerySaveSupport() {

					@Override
					public void onSaveQuery(AjaxRequestTarget target, String query) {
						new ModalPanel(target)  {

							@Override
							protected Component newContent(String id) {
								return new SaveQueryPanel(id) {

									@Override
									protected void onSaveForMine(AjaxRequestTarget target, String name) {
										QuerySetting<NamedProjectQuery> querySetting = getLoginUser().getProjectQuerySetting();
										NamedProjectQuery namedQuery = NamedQuery.find(querySetting.getUserQueries(), name);
										if (namedQuery == null) {
											namedQuery = new NamedProjectQuery(name, query);
											querySetting.getUserQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										OneDev.getInstance(UserManager.class).save(getLoginUser());
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onSaveForAll(AjaxRequestTarget target, String name) {
										GlobalProjectSetting projectSetting = getProjectSetting();
										NamedProjectQuery namedQuery = projectSetting.getNamedQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedProjectQuery(name, query);
											projectSetting.getNamedQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										OneDev.getInstance(SettingManager.class).saveProjectSetting(projectSetting);
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onCancel(AjaxRequestTarget target) {
										close();
									}
									
								};
							}
							
						};
					}

					@Override
					public boolean isSavedQueriesVisible() {
						savedQueries.configure();
						return savedQueries.isVisible();
					}

				};
			}

		});
		
	}
	
	public static PageParameters paramsOf(@Nullable String query, int page, int expectedCount) {
		PageParameters params = new PageParameters();
		if (query != null)
			params.add(PARAM_QUERY, query);
		if (page != 0)
			params.add(PARAM_PAGE, page);
		if (expectedCount != 0)
			params.add(PARAM_EXPECTED_COUNT, expectedCount);
		return params;
	}
	
	public static PageParameters paramsOf(int page, int expectedCount) {
		String query = null;
		
		User user = SecurityUtils.getUser();
		if (user != null && !user.getProjectQuerySetting().getUserQueries().isEmpty()) {
			query = user.getProjectQuerySetting().getUserQueries().iterator().next().getQuery();
		} else {
			if (!getProjectSetting().getNamedQueries().isEmpty())
				query = getProjectSetting().getNamedQueries().iterator().next().getQuery();
		}
		return paramsOf(query, page, expectedCount);
	}
	
}
