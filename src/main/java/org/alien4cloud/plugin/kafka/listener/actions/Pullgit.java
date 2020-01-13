package org.alien4cloud.plugin.kafka.listener.actions;

import alien4cloud.csar.services.CsarGitRepositoryService;
import alien4cloud.csar.services.CsarGitService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.model.Csar;

import org.alien4cloud.plugin.kafka.listener.model.Action;

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class Pullgit extends AbstractAction {

    @Inject
    private CsarGitService csarGitService;
    @Inject
    private CsarGitRepositoryService csarGitRepositoryService;

    public Action process (Action action) {
       String url = safe(action.getParameters()).get("url");
       String branch = safe(action.getParameters()).get("branch");

       Action response = initResponse(action);

       /* url is mandatory */
       if (StringUtils.isBlank(url)) {
          log.error ("Request:" + action.getRequestid() + " - No url defined for pull git action");
          return completeResponse(response, "KO");
       }

       /* repository parameters */
       String id = null;
       List<CsarGitCheckoutLocation> branches = null;
       String username = null, password = null;
       boolean local = false;

       /* get repository */
       String query = url;
       GetMultipleDataResult<CsarGitRepository> srepo = csarGitRepositoryService.search (query, 0, 1);
       if (srepo.getTotalResults() == 0) {
          if (StringUtils.isBlank(branch)) {
             log.error ("Request:" + action.getRequestid() + " - Repository does not exist: branch is mandatory");
             return completeResponse(response, "KO");
          }
          log.info ("Request:" + action.getRequestid() + " - creating repository " + url + "[" + branch + "]");

          branches = new ArrayList<CsarGitCheckoutLocation>();
          CsarGitCheckoutLocation nb = new CsarGitCheckoutLocation();
          nb.setBranchId(branch);
          branches.add(nb);

          username = safe(action.getParameters()).get("user");
          password = safe(action.getParameters()).get("password");

          id = csarGitRepositoryService.create (url, username, password, branches, false);
       } else {
          CsarGitRepository repo = srepo.getData()[0];
          branches = repo.getImportLocations();
          id = repo.getId();
          username = repo.getUsername();
          password = repo.getPassword();
          local = repo.isStoredLocally();
       }

       /* check branch if any */
       if (StringUtils.isNotBlank(branch)) {
          boolean found = false;
          for (CsarGitCheckoutLocation ibranch : branches) {
             found = ibranch.getBranchId().equals(branch);
             if (found) {
                break;
             }
          }
          if (!found) { // update repository
             log.info ("Request:" + action.getRequestid() + " - adding branch " + branch + " to repository");
             CsarGitCheckoutLocation nb = new CsarGitCheckoutLocation();
             nb.setBranchId(branch);
             branches.add(nb);
             csarGitRepositoryService.update (id, url, username, password, branches, local);
          }
       }

       log.info ("Request:" + action.getRequestid() + " - Pulling GIT " + id + " : BEGIN");
       List<ParsingResult<Csar>> parsingResult = csarGitService.importFromGitRepository(id);
       for (ParsingResult<Csar> result : parsingResult) {
           // check if there is any critical failure in the import
           for (ParsingError error : result.getContext().getParsingErrors()) {
               if (ParsingErrorLevel.ERROR.equals(error.getErrorLevel())) {
                  log.error ("Error while importing "  + result.getResult().getName() + " : " + error.getProblem());
               }
           }
       }
       log.info ("Request:" + action.getRequestid() + " - Pulling GIT " + id + " : END");

       return completeResponse(response, "OK");
    }

}
