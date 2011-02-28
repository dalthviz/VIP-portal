/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.creatis.insa-lyon.fr/~silva
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.vip.portal.server.rpc;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import fr.insalyon.creatis.vip.portal.client.bean.Authentication;
import fr.insalyon.creatis.vip.portal.client.bean.Configuration;
import fr.insalyon.creatis.vip.portal.client.bean.User;
import fr.insalyon.creatis.vip.portal.client.rpc.ConfigurationService;
import fr.insalyon.creatis.vip.portal.server.business.BusinessException;
import fr.insalyon.creatis.vip.portal.server.business.proxy.MyProxyClient;
import fr.insalyon.creatis.vip.portal.server.dao.DAOException;
import fr.insalyon.creatis.vip.portal.server.dao.DAOFactory;
import fr.insalyon.creatis.vip.common.server.ServerConfiguration;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Rafael Silva
 */
public class ConfigurationServiceImpl extends RemoteServiceServlet implements ConfigurationService {

    public Configuration loadConfiguration() {

        HttpServletRequest request = this.getThreadLocalRequest();
        Object o = request.getAttribute("javax.servlet.request.X509Certificate");
        Authentication authentication = null;

        if (o != null) {
            X509Certificate certs[] = (X509Certificate[]) o;
            X509Certificate cert = certs[0];

            String[] subjectDN = cert.getSubjectDN().getName().split(", ");
            String userDN = "";

            for (int i = subjectDN.length - 1; i >= 0; i--) {
                userDN += "/" + subjectDN[i];
            }

            if (DAOFactory.getDAOFactory().getUserDAO().exists(userDN)) {
                User user = DAOFactory.getDAOFactory().getUserDAO().getUser(userDN);
                HttpSession session = request.getSession();
                if (session.getAttribute("userDN") == null) {
                    session.setAttribute("userDN", userDN);
                }

                String proxyFileName = "";
                try {
                    MyProxyClient myproxy = new MyProxyClient();
                    proxyFileName = myproxy.getProxy(user.getCanonicalName(), userDN);
                    authentication = new Authentication(
                        user.getCanonicalName() + " / " + user.getOrganizationUnit(),
                        userDN, user.getGroups(), proxyFileName, true);

                } catch (BusinessException ex) {
                    authentication = new Authentication(
                            user.getCanonicalName() + " / " + user.getOrganizationUnit(),
                            userDN, new HashMap(), "", false);
                }
            }
        } else {
            authentication = new Authentication("Anonymous", "Anonymous", new HashMap(), "", false);
        }

        ServerConfiguration conf = ServerConfiguration.getInstance();
        URI uri = null;
        try {
            uri = new URI(conf.getMoteurServer());
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return new Configuration(authentication, 
                conf.getQuickStartURL(),
                uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort());
    }

    public String addGroup(String groupName) {
        try {
            return DAOFactory.getDAOFactory().getGroupDAO().add(groupName);
        } catch (DAOException ex) {
            return ex.getMessage();
        }
    }

    public String updateGroup(String oldName, String newName) {
        try {
            return DAOFactory.getDAOFactory().getGroupDAO().update(oldName, newName);
        } catch (DAOException ex) {
            return ex.getMessage();
        }
    }

    public void removeGroup(String groupName) {
        try {
            DAOFactory.getDAOFactory().getGroupDAO().remove(groupName);
        } catch (DAOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<String> getGroups() {
        return DAOFactory.getDAOFactory().getGroupDAO().getGroups();
    }

    public String addUser(User user) {
        return DAOFactory.getDAOFactory().getUserDAO().add(user);
    }

    public String updateUser(User user) {
        return DAOFactory.getDAOFactory().getUserDAO().update(user);
    }

    public void removeUser(String dn) {
        DAOFactory.getDAOFactory().getUserDAO().remove(dn);
    }

    public List<User> getUsers() {
        return DAOFactory.getDAOFactory().getUserDAO().getUsers();
    }

    public User getUser(String dn) {
        return DAOFactory.getDAOFactory().getUserDAO().getUser(dn);
    }
}
