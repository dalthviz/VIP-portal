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
package fr.insalyon.creatis.vip.api.rest.controller.data;

import fr.insalyon.creatis.vip.api.business.*;
import fr.insalyon.creatis.vip.api.rest.RestApiBusiness;
import fr.insalyon.creatis.vip.api.rest.model.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.List;

/**
 * Created by abonnet on 1/13/17.
 */
@RestController
@RequestMapping("path")
public class DataController {

    public static final Logger logger = Logger.getLogger(DataController.class);

    @Autowired
    private RestApiBusiness restApiBusiness;

    @Autowired
    private DataApiBusiness dataApiBusiness;

    // although the controller is a singleton, these are proxies that always point on the current request
    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(path = "/**", params = "action=properties")
    public PathProperties getPathProperties()
            throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("getPathProperties", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        return dataApiBusiness.getPathProperties(completePath);
    }

    @RequestMapping(path = "/**", params = "action=exists")
    public ExistsApiResponse doesPathExists() throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("doesPathExists", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        return new ExistsApiResponse(dataApiBusiness.doesFileExist(completePath));
    }

    @RequestMapping(path = "/**", params = "action=list")
    public List<PathProperties> listDirectory() throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("listDirectory", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        return dataApiBusiness.listDirectory(completePath);
    }

    @RequestMapping(path = "/**", params = "action=md5")
    public void getFileMD5() throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("getFileMD5", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        // TODO implement that
        throw new ApiException("Not implemented");
    }

    @RequestMapping(path = "/**", params = "action=content")
    public ResponseEntity<FileSystemResource> downloadRawFile() throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("downloadFile", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        File file = dataApiBusiness.getFile(completePath);
        FileSystemResource res = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        // TODO improve mime-type
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(res, headers, HttpStatus.OK);
    }

    @RequestMapping(path = "/**", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePath() throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("deletePath", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        dataApiBusiness.deletePath(completePath);
    }

    /*
    @RequestMapping(path="directory", method = RequestMethod.POST, params="uri")
    public PathProperties mkdir(@RequestParam String uri) throws ApiException {
        // common stuff
        ApiUtils.methodInvocationLog("mkdir", getCurrentUserEmail(), uri);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        return dataApiBusiness.mkdir(uri);
    }      */

    @RequestMapping(path = "/**", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadFile(InputStream requestInputStream) throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("uploadFile", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        dataApiBusiness.uploadRawFileFromInputStream(completePath, requestInputStream);
        // TODO : think about returning the PahtProperties of the created Path, to be informed of a filename change
    }

    @RequestMapping(path = "/**", method = RequestMethod.PUT, consumes = "application/carmin+json")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadCustomData(@RequestBody UploadData uploadData) throws ApiException {
        String completePath = extractWildcardPath(httpServletRequest);
        ApiUtils.methodInvocationLog("uploadCustomData", getCurrentUserEmail(), completePath);
        restApiBusiness.getApiContext(httpServletRequest, true);
        // business call
        dataApiBusiness.uploadCustomData(completePath, uploadData);
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static String extractWildcardPath(HttpServletRequest request) {
        String prefixToSearch = "/rest/path/"; // TODO : parametize that
        int index = request.getRequestURI().indexOf(prefixToSearch);
        // "-1" at the end to keep the beginning slash
        return request.getRequestURI().substring(index + prefixToSearch.length() - 1);
    }
}

