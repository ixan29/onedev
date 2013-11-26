package com.pmease.gitop.core.gatekeeper;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.gitop.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitop.core.model.PullRequest;

@SuppressWarnings("serial")
@Editable
public class IsFastForward extends AbstractGateKeeper {

	@Override
	public CheckResult check(PullRequest request) {
		if (request.isFastForward())
			return accepted("Is fast-forward.");
		else
			return rejected("None fast-forward.");
	}

}
