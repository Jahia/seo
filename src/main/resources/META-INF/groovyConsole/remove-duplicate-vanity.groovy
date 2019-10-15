import org.jahia.api.Constants
import org.jahia.services.content.*
import org.jahia.services.content.decorator.JCRSiteNode
import org.jahia.services.sites.JahiaSite

import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.query.Query

//String siteKey = "digitall";
//boolean removeDuplicateVanity = false; // set to true to remove duplicated vanities

Set<String> urls = new HashSet<String>();
Set<String> duplicateUrls = new HashSet<String>();
def logger = log;


List<JCRSiteNode> sitesNodeList = org.jahia.services.sites.JahiaSitesService.getInstance().getSitesNodeList();
Iterator<String> sitesNodeListIterator = sitesNodeList.iterator();

while(sitesNodeListIterator.hasNext()) {
    JCRSiteNode siteNode = sitesNodeListIterator.next();
    String siteKey = siteNode.getName();
    logger.info("##### Check duplicate vanity on site [" + siteKey + "]");
    JahiaSite site = org.jahia.services.sites.JahiaSitesService.getInstance().getSiteByKey(siteKey);
    JCRTemplate.getInstance().doExecuteWithSystemSession(null, Constants.EDIT_WORKSPACE, new JCRCallback() {
        @Override
        Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
            def q = "select * from [jnt:vanityUrl] as p where isdescendantnode('/sites/" + siteKey + "')";
            //logger.info("Here are the vanity urls")
            NodeIterator iterator = session.getWorkspace().getQueryManager().createQuery(q, Query.JCR_SQL2).execute().getNodes();
            while (iterator.hasNext()) {
                try {
                    final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                    String url = node.getPropertyAsString('j:url');
                    //logger.info(url)
                    if (urls.contains(url)) {
                        duplicateUrls.add(url);
                    } else {
                        urls.add(url);
                    }
                } catch (javax.jcr.PathNotFoundException e) {

                } catch (javax.jcr.ItemNotFoundException inf) {

                }
            }
            if (duplicateUrls.size() == 0) {
                logger.info("Great, we did not found any duplicate vanity!")
            } else {
                logger.info("Mmm.... we found some duplicate entries...")
                for (String duplicateUrl : duplicateUrls) {
                    logger.info("[" + duplicateUrl + "]");
                    def u = "select * from [jnt:vanityUrl] where ['j:url']='" + duplicateUrl + "' and isdescendantnode('/sites/" + siteKey + "') order by ['jcr:created']";
                    iterator = session.getWorkspace().getQueryManager().createQuery(u, Query.JCR_SQL2).execute().getNodes();
                    int i = 0;
                    while (iterator.hasNext()) {
                        try {
                            final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                            if (i > 0) {
                                if (removeDuplicateVanity) {
                                    logger.info("  - " + node.getPath() + " (removed)");
                                    node.remove();
                                } else {
                                    logger.info("  - " + node.getPath() + " (conflict found: this vanity should be removed)");
                                }

                            } else {
                                logger.info("  - " + node.getPath());
                            }
                            i++;
                        } catch (javax.jcr.PathNotFoundException e) {

                        } catch (javax.jcr.ItemNotFoundException inf) {

                        }
                    }
                }
            }
            if (removeDuplicateVanity) {
                session.save();
            }
            logger.info("");
            return null;
        }
    });
}

