/*
 * Copyright and authors: see LICENSE.txt in base repository.
 *
 * This software is a web portal for pipeline execution on distributed systems.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
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
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.insalyon.creatis.vip.datamanager.server.rpc;

import fr.insalyon.creatis.devtools.zip.UnZipper;
import fr.insalyon.creatis.grida.client.GRIDAClient;
import fr.insalyon.creatis.grida.client.GRIDAClientException;
import fr.insalyon.creatis.grida.client.GRIDAPoolClient;
import fr.insalyon.creatis.vip.core.client.bean.User;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants;
import fr.insalyon.creatis.vip.core.server.business.CoreUtil;
import fr.insalyon.creatis.vip.datamanager.client.view.DataManagerException;
import fr.insalyon.creatis.vip.datamanager.server.DataManagerUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class FileUploadServiceImpl extends HttpServlet {

    private static Logger logger = Logger.getLogger(FileUploadServiceImpl.class);
    private GRIDAClient client;
    private GRIDAPoolClient poolClient;
    private User user;
    private String path;
    private boolean usePool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        this.user = (User) request.getSession().getAttribute(CoreConstants.SESSION_USER);
        if (this.user != null && ServletFileUpload.isMultipartContent(request)) {

            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List items = upload.parseRequest(request);
                Iterator iter = items.iterator();
                String fileName = null;
                FileItem fileItem = null;
                this.path = null;
                String target = "uploadComplete";
                boolean single = true;
                boolean unzip = true;
                this.usePool = true;
                String operationID = "no-id";

                while (iter.hasNext()) {
                    FileItem item = (FileItem) iter.next();

                    switch (item.getFieldName()) {
                        case "path":
                            this.path = item.getString();
                            break;
                        case "file":
                            fileName = item.getName();
                            fileItem = item;
                            break;
                        case "target":
                            target = item.getString();
                            break;
                        case "single":
                            single = Boolean.valueOf(item.getString());
                            break;
                        case "unzip":
                            unzip = Boolean.valueOf(item.getString());
                            break;
                        case "pool":
                            this.usePool = Boolean.valueOf(item.getString());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid FieldName: " + item.getFieldName());
                    }

                }
                if (fileName != null && !fileName.equals("")) {

                    boolean local = this.path.equals("local") ? true : false;
                    String rootDirectory = DataManagerUtil.getUploadRootDirectory(local);
                    fileName = new File(fileName).getName().trim().replaceAll(" ", "_");
                    fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                    File uploadedFile = new File(rootDirectory + fileName);

                    try {
                        fileItem.write(uploadedFile);
                        response.getWriter().write(fileName);

                        if (!local) {
                            // GRIDA Client
                            logger.info("(" + user.getEmail() + ") Uploading '" + uploadedFile.getAbsolutePath() + "' to '" + path + "'.");
                            if (usePool) {
                                poolClient = CoreUtil.getGRIDAPoolClient();
                            } else {
                                client = CoreUtil.getGRIDAClient();
                            }
                            if (single || !unzip) {
                                operationID = uploadFile(uploadedFile.getAbsolutePath(), path);
                            } else {
                                UnZipper.unzip(uploadedFile.getAbsolutePath());
                                String dir = uploadedFile.getParent();
                                uploadedFile.delete();
                                operationID = processDir(dir, path);
                            }

                        } else {
                            operationID = fileName;
                            logger.info("(" + user.getEmail() + ") Uploaded '" + uploadedFile.getAbsolutePath() + "'.");
                        }
                    } catch (Exception ex) {
                        logger.error("Error uploading a file", ex);
                    }
                }
                //TODO: change the HTML/JS response to XML data that could be directly processed in JS
                response.setContentType("text/html");
                response.setHeader("Pragma", "No-cache");
                response.setDateHeader("Expires", 0);
                response.setHeader("Cache-Control", "no-cache");
                PrintWriter out = response.getWriter();
                out.println("<html>");
                out.println("<body>");
                out.println("<script type=\"text/javascript\" id=\"runscript\">");
                out.println("if (parent." + target + ") parent." + target + "('"
                        + operationID + "');");
                out.println("</script>");
                out.println("</body>");
                out.println("</html>");
                out.flush();

            } catch (FileUploadException ex) {
                logger.error("Error uploading a file", ex);
            }
        }
    }

    private String processDir(String dir, String baseDir)
            throws GRIDAClientException, DataManagerException {

        StringBuilder ids = new StringBuilder();
        for (File f : new File(dir).listFiles()) {
            if (f.isDirectory()) {
                ids.append(processDir(f.getAbsolutePath(), baseDir + "/" + f.getName()));
            } else {
                ids.append(uploadFile(f.getAbsolutePath(), baseDir));
            }
            ids.append("##");
        }
        return ids.toString();
    }

    private String uploadFile(String fileName, String dir)
            throws GRIDAClientException, DataManagerException {

        String parsed = fileName.trim().replaceAll(" ", "_");
        parsed = Normalizer.normalize(parsed, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        if (!parsed.equals(fileName)) {
            new File(fileName).renameTo(new File(parsed));
            fileName = parsed;
        }

        logger.info("(" + user.getEmail() + ") Uploading '" + fileName + "' to '" + dir + "'.");
        if (usePool) {
            return poolClient.uploadFile(fileName,
                    DataManagerUtil.parseBaseDir(user, dir), user.getEmail());
        } else {
            client.uploadFile(fileName, DataManagerUtil.parseBaseDir(user, dir));
            return "no-id";
        }
    }
}
