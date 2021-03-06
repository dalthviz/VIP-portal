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
package fr.insalyon.creatis.vip.query.server.dao.business;

import fr.insalyon.creatis.vip.query.client.bean.Query;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.core.server.dao.mysql.PlatformConnection;
import fr.insalyon.creatis.vip.query.client.bean.Parameter;
import fr.insalyon.creatis.vip.query.client.bean.QueryExecution;
import fr.insalyon.creatis.vip.query.client.bean.QueryVersion;
import fr.insalyon.creatis.vip.query.client.bean.Value;
import static fr.insalyon.creatis.vip.query.server.dao.business.QueryData.html2text;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

/**
 *
 * @author Boujelben
 */
public class QueryData implements QueryDAO {

    private final static Logger logger = Logger.getLogger(QueryData.class);
    private Connection connection;

    public QueryData() throws DAOException {
        connection = PlatformConnection.getInstance().getConnection();
    }

    @Override
    public List<String[]> getQueries(String creator) throws DAOException {

        try {

            PreparedStatement ps = connection.prepareStatement("SELECT q.queryName, v.queryVersionID, v.queryVersion, v.dateCreation, v.queryID FROM Query q, QueryVersion v where q.queryID=v.queryID AND (q.queryMaker=? OR v.isPublic=?)");
            ps.setString(1, creator);
            ps.setInt(2, 1);
            ResultSet rs = ps.executeQuery();
            List<String[]> queries = new ArrayList<String[]>();

            while (rs.next()) {

              
                    Timestamp date = rs.getTimestamp("dateCreation");
                    Integer version = rs.getInt("queryVersion");
                    Long qID = rs.getLong("queryID");


                    queries.add(new String[]{rs.getString("queryName"), date.toString(), version.toString(), rs.getString("queryVersionID"), qID.toString()});
               

            }
            ps.close();
            return queries;




            /*
             try {
             PreparedStatement ps = connection.prepareStatement("SELECT "
             + "QueryVersion.queryID, queryName, dateCreation, queryVersion, queryVersionID FROM "
             + "Query, QueryVersion "
             + "WHERE Query.queryID=QueryVersion.queryID "
             + "ORDER BY dateCreation DESC");

             ResultSet rs = ps.executeQuery();
             List<String[]> queries = new ArrayList<String[]>();

             while (rs.next()) {
             Integer version = rs.getInt("queryVersion");
             Timestamp date = rs.getTimestamp("dateCreation");
             Long qID = rs.getLong("QueryVersion.queryID");
                
             queries.add(new String[]{rs.getString("queryName"), date.toString(), version.toString(), rs.getString("queryVersionID"),qID.toString()});
             }
             ps.close();
             return queries;
             * */
        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
   public List<String> getDescriptionQueryMaker(Long queryVersionID) throws DAOException {

        try {

             List<String> values = new ArrayList<String>();
            PreparedStatement ps2 = connection.prepareStatement("SELECT queryMaker,description FROM QueryVersion qv,Query q WHERE q.queryID=qv.queryID AND queryVersionID=?");


            ps2.setLong(1, queryVersionID);
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
              // values.add(0,rs2.getString("queryMaker") );
              
               PreparedStatement prep = connection.prepareStatement("SELECT first_name,last_name FROM VIPUsers WHERE email=?");
              prep.setString(1, rs2.getString("queryMaker"));
              ResultSet rsPrep = prep.executeQuery();
               while (rsPrep.next()) {
              values.add(0,rsPrep.getString("first_name")+" "+rsPrep.getString("last_name")+" ("+rs2.getString("queryMaker")+")" );
               }
              values.add(1,rs2.getString("description") );

               
            }
            ps2.close();



            return values;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public List<String[]> getQuerie(Long queryversionid, Long queryID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT queryName, queryID FROM Query  WHERE queryID=? ");
            //
            ps.setLong(1, queryID);
            ResultSet rs = ps.executeQuery();

            List<String[]> queries = new ArrayList<String[]>();

            while (rs.next()) {
                PreparedStatement ps2 = connection.prepareStatement("SELECT body,description,isPublic FROM QueryVersion  WHERE queryVersionID=? ");
                ps2.setLong(1, queryversionid);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    Long qID = rs.getLong("queryID");
                    String isPublic= String.valueOf(rs2.getInt("isPublic"));
                    
                    queries.add(new String[]{rs.getString("queryName"), rs2.getString("description"), rs2.getString("body"), qID.toString(),isPublic });
                }
                ps2.close();
            }


            ps.close();
            return queries;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public List<String[]> getVersion() throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT "
                    + "queryName,dateCreation,queryVersion FROM"
                    + " Query query,QueryVersion queryversion "
                    + "WHERE query.queryID=queryVersion.queryID"
                    + "ORDER BY queryVersion.dateCreation");

            ResultSet rs = ps.executeQuery();
            List<String[]> queries = new ArrayList<String[]>();

            while (rs.next()) {
                Long version = rs.getLong("queryVersion");
                queries.add(new String[]{rs.getString("queryName"), rs.getString("dateCreation"), version.toString()});
            }
            ps.close();
            return queries;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public List<String[]> getParameterValue(Long queryExecutionID) throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT "
                    + "value,parameterID From Value WHERE queryExecutionID=?");
            ps.setLong(1, queryExecutionID);

            ResultSet rs = ps.executeQuery();
            List<String[]> queries = new ArrayList<String[]>();

            while (rs.next()) {
                Long parameterID = rs.getLong("parameterID");
                PreparedStatement ps2 = connection.prepareStatement("SELECT "
                        + "name From Parameter WHERE parameterID=?");
                ps2.setLong(1, parameterID);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    queries.add(new String[]{rs2.getString("name"), rs.getString("value")});
                }
                ps2.close();

            }
            ps.close();
            return queries;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void removeVersion(Long versionid) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("DELETE "
                    + "FROM QueryVersion WHERE queryVersionID=?");


            ps.setLong(1, versionid);

            ps.execute();
            ps.close();

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void removeQueryExecution(Long executionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("DELETE "
                    + "FROM QueryExecution WHERE queryExecutionID=?");


            ps.setLong(1, executionID);

            ps.execute();
            ps.close();

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public Long maxVersion(Long queryID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("select max(queryVersion) from QueryVersion WHERE queryID=?");


            Long max = 0L;
            ps.setLong(1, queryID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                max = rs.getLong(1);
            }
            ps.close();
            return max;
        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public Long getQueryID(Long queryVersionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("select queryID from QueryVersion WHERE queryVersionID=?");



            ps.setLong(1, queryVersionID);
            ResultSet rs = ps.executeQuery();
            Long queryID = 0L;
            if (rs.next()) {
                queryID = rs.getLong(1);
            }
            ps.close();
            return queryID;
        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public List<Long> addParameter(Parameter param) throws DAOException {

        try {
            PreparedStatement pss = connection.prepareStatement("SELECT body from QueryVersion WHERE queryVersionID=? ");
            pss.setLong(1, param.getQueryVersionID());

            ResultSet rss = pss.executeQuery();
            String body = null;
            while (rss.next()) {
                body = rss.getString("body");
            }
            //String str[]=body.split("\\[");
            List<String> listparam = new ArrayList<String>();
            char b = 'a';

            char last = 'a';

            for (int i = 0; i < body.length(); i++) {
                b = body.charAt(i);
                if (b == '[') {
                    int k = 0;
                    for (int j = i; j < body.length(); j++) {
                        last = body.charAt(j);

                        if (last == ']' && k == 0) {
                            listparam.add(body.substring(i + 1, j));
                            k = 1;

                        }
                    }
                }
            }
            List<Long> parametersid = new ArrayList<Long>();
            for (String s : listparam) {
                String str[] = s.split("\\;");
                try {
                    PreparedStatement ps;
                    ps = connection.prepareStatement(
                            "INSERT INTO Parameter(name, type, description, example, queryVersionID) "
                            + "VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setString(1, str[0]);
                    ps.setString(2, str[1]);
                    ps.setString(3, str[2]);
                    ps.setString(4, str[3]);
                    ps.setLong(5, param.getQueryVersionID());
                    ps.execute();
                    ResultSet rs = ps.getGeneratedKeys();
                    Long idAuto_increment = new Long(0);
                    while (rs.next()) {
                        idAuto_increment = rs.getLong(1);
                    }

                    ps.close();

                    parametersid.add(idAuto_increment);
                } catch (SQLException ex) {
                    logger.error(ex);
                    throw new DAOException(ex);
                }

            }




            return parametersid;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

//methode ajoute un query et retourne un long l'id de la requete
    @Override
    public Long add(Query query) throws DAOException {
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(
                    "INSERT INTO Query(queryName, queryMaker) "
                    + "VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);


            ps.setString(1, query.getName());
            ps.setString(2, query.getQueryMaker());

            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            Long idAuto_increment = new Long(0);
            while (rs.next()) {
                idAuto_increment = rs.getLong(1);
            }

            ps.close();
            return idAuto_increment;


        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public Long addVersion(QueryVersion version,boolean bodyTypeHtml) throws DAOException {
        try {

            PreparedStatement ps2 = connection.prepareStatement(
                    "INSERT INTO QueryVersion(queryVersion, queryID, body, dateCreation,description,isPublic) VALUES (?, ?, ?, ?, ?, ? )", PreparedStatement.RETURN_GENERATED_KEYS);
            ps2.setLong(1, version.getQueryVersion());
            ps2.setObject(2, version.getQueryID());
            if (bodyTypeHtml==true) {
                ps2.setString(3, html2text(version.getBody()));
                
            } else {
                ps2.setString(3, version.getBody());        
            }
            ps2.setTimestamp(4, getCurrentTimeStamp());
            ps2.setString(5, version.getDescription());
            int  isPublicVersion;
            if(version.isIsPublic()){
            isPublicVersion=1;}
            else{
            isPublicVersion=0;
            }
            ps2.setInt(6, isPublicVersion);
            ps2.execute();
            ResultSet rs = ps2.getGeneratedKeys();
            Long idAuto_increment = new Long(0);
            while (rs.next()) {
                idAuto_increment = rs.getLong(1);
            }

            ps2.close();
            return idAuto_increment;
        } catch (SQLException ex) {

            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public List<Parameter> getParameter(Long queryVersionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT "
                    + "parameterID, name, type, description, example "
                    + "FROM Parameter "
                    + "WHERE queryVersionID = ? ORDER BY parameterID DESC");

            ps.setLong(1, queryVersionID);
            ResultSet rs = ps.executeQuery();
            List<Parameter> parameters = new ArrayList<Parameter>();

            while (rs.next()) {
                parameters.add(new Parameter(rs.getString("name"), rs.getString("type"), rs.getString("description"), rs.getString("example"), rs.getLong("parameterID")));
            }

            ps.close();
            return parameters;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public void updateQueryExecution(String bodyResult, String status, Long executionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE "
                    + "QueryExecution "
                    + "SET bodyResult=?, status=? "
                    + "WHERE queryExecutionID=?");



            ps.setString(1, bodyResult);
            ps.setString(2, status);
            ps.setLong(3, executionID);
            ps.executeUpdate();
            ps.close();



        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void updateQueryExecutionStatusWaiting(String status, Long executionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE "
                    + "QueryExecution "
                    + "SET dateExecution=?, status=? "
                    + "WHERE queryExecutionID=?");
            ps.setTimestamp(1, getCurrentTimeStamp());
            ps.setString(2, status);
            ps.setLong(3, executionID);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void updateQueryExecutionStatusFailed(String status, Long executionID) throws DAOException {

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE "
                    + "QueryExecution "
                    + "SET dateEndExecution=?, status=?, pathFileResult=? "
                    + "WHERE queryExecutionID=?");



            ps.setTimestamp(1, getCurrentTimeStamp());
            ps.setString(2, status);

            ps.setString(3, "Query Execution was interrupted by the user");
            ps.setLong(4, executionID);
            ps.executeUpdate();
            ps.close();



        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }
//update without body
    @Override
    public void updateQueryVersion(Long queryVersionID, String name, String description, boolean isPublic) throws DAOException {

        try {
            PreparedStatement ps1 = connection.prepareStatement(
                    "select queryID from QueryVersion where queryVersionID=?");
            ps1.setLong(1, queryVersionID);
            ResultSet rs = ps1.executeQuery();
            Long queryID = 0L;
            while (rs.next()) {
                queryID = rs.getLong("queryID");
            }

            ps1.close();
            PreparedStatement ps = connection.prepareStatement("UPDATE "
                    + "Query, QueryVersion "
                    + "SET description=?, queryName=?, isPublic=? "
                    + "WHERE Query.queryID=? AND QueryVersion.queryVersionID=?");



            ps.setString(1, description);
            ps.setString(2, name);
           
          
            if (isPublic) {
                ps.setInt(3, 1);
            } else {
                 ps.setInt(3, 0);
            }
            ps.setLong(4, queryID);
            ps.setLong(5, queryVersionID);
            ps.executeUpdate();
            ps.close();



        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public Long addValue(Value value) throws DAOException {
        try {

            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Value(value, parameterID, queryExecutionID) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, value.getValue());
            ps.setLong(2, value.getParameterID());
            ps.setLong(3, value.getQueryExecutionID());

            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            Long idAuto_increment = new Long(0);
            while (rs.next()) {
                idAuto_increment = rs.getLong(1);
            }

            ps.close();
            return idAuto_increment;
        } catch (SQLException ex) {

            logger.error(ex);
            throw new DAOException(ex);
        }

    }

    @Override
    public Long addQueryExecution(QueryExecution queryExecution) throws DAOException {
         if(queryExecution.getQueryVersionID()!=null){
        try {
           
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO QueryExecution(queryVersionID, dateExecution, executer,status, name, bodyResult) VALUES (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1,queryExecution.getQueryVersionID()); 
            ps.setTimestamp(2, getCurrentTimeStamp());
            ps.setString(3, queryExecution.getExecuter());
            ps.setString(4, queryExecution.getStatus());
            
            if (queryExecution.getName()==null || queryExecution.getName() == ""||queryExecution.getName().isEmpty()) {
                queryExecution.setName("empty name");
            }
            
            ps.setString(5, queryExecution.getName());
            ps.setString(6, queryExecution.getBodyResult());   
            
          
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            Long idAuto_increment = new Long(0);
            while (rs.next()) {
                idAuto_increment = rs.getLong(1);
            }

            ps.close();
            return idAuto_increment;
        } catch (SQLException ex) {

            logger.error(ex);
            throw new DAOException(ex);
        }
          }
         //by query explorer
          else{
                try {
           
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO QueryExecution(dateExecution, executer,status, name, bodyResult) VALUES (?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
           
            ps.setTimestamp(1, getCurrentTimeStamp());
            ps.setString(2, queryExecution.getExecuter());
            ps.setString(3, queryExecution.getStatus());
             if (queryExecution.getName()==null || queryExecution.getName() == ""||queryExecution.getName().isEmpty()) {
                queryExecution.setName("empty name");
            }
            ps.setString(4, queryExecution.getName());     
            ps.setString(5, html2text(queryExecution.getBodyResult()));    
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            Long idAuto_increment = new Long(0);
            while (rs.next()) {
                idAuto_increment = rs.getLong(1);
            }

            ps.close();
            return idAuto_increment;
        } catch (SQLException ex) {

            logger.error(ex);
            throw new DAOException(ex);
        }
          
          
          
         }
          
    }

    @Override
    public List<String[]> getQueryHistory(String executer, String state) throws DAOException {
        if (state.equals("admin")) {

            try {
                PreparedStatement ps = connection.prepareStatement("SELECT "
                        + "queryExecutionID,name,queryName,queryVersion,executer,dateExecution,status,bodyResult,pathFileResult,dateEndExecution FROM "
                        + "Query query,QueryVersion queryversion,QueryExecution queryexe WHERE "
                        + "query.queryID=queryversion.queryID AND queryversion.queryVersionID=queryexe.queryVersionID "
                        + "UNION "
                        + "SELECT "
                        + "queryExecutionID,name,name as queryName,queryVersionID,executer,dateExecution,status,bodyResult,pathFileResult,dateEndExecution FROM "
                        + "QueryExecution queryexe WHERE "
                        + "queryexe.queryVersionID IS NULL "  
                        + "ORDER BY dateExecution DESC");
                
               
          

                ResultSet rs = ps.executeQuery();
             
               
                List<String[]> queries = new ArrayList<String[]>();

                while (rs.next()) {
                    PreparedStatement ps2 = connection.prepareStatement("SELECT "
                            + "first_name, last_name FROM VIPUsers WHERE email=?");
                    ps2.setString(1, rs.getString("executer"));
                    ResultSet rs2 = ps2.executeQuery();
                    String fullNameExecuter = new String();
                    while (rs2.next()) {

                        fullNameExecuter = rs2.getString("first_name") + " " + rs2.getString("last_name");
                    }


                    Timestamp date = rs.getTimestamp("dateExecution");
                    String dateEnd = rs.getString("dateEndExecution");
                    Long id = rs.getLong("queryExecutionID");
                    Long version = rs.getLong("queryVersion");
                    queries.add(new String[]{id.toString(), rs.getString("name"), rs.getString("queryName"),version.toString(), fullNameExecuter, date.toString(), rs.getString("status"), rs.getString("bodyResult"), rs.getString("pathFileResult"), dateEnd});
                }
                ps.close();
                return queries;

            } catch (SQLException ex) {
                logger.error(ex);
                throw new DAOException(ex);
            }
        } else  {

            try {
                PreparedStatement ps = connection.prepareStatement("SELECT "
                        + "queryExecutionID,name,queryName,queryVersion,executer,dateExecution,status,bodyResult,pathFileResult,dateEndExecution FROM "
                        + "Query query,QueryVersion queryversion,QueryExecution queryexe WHERE "
                        + "query.queryID=queryversion.queryID AND queryversion.queryVersionID=queryexe.queryVersionID AND queryexe.executer=? "
                        + "UNION "
                        + "SELECT "
                        + "queryExecutionID,name,name as queryName,queryVersionID,executer,dateExecution,status,bodyResult,pathFileResult,dateEndExecution FROM "
                        + "QueryExecution queryexe WHERE "
                        + "queryexe.queryVersionID IS NULL AND executer=? "  
                        + "ORDER BY dateExecution DESC");
                
               
                ps.setString(1, executer);
                ps.setString(2, executer);
                ResultSet rs = ps.executeQuery();

                List<String[]> queries = new ArrayList<String[]>();

                while (rs.next()) {
                    PreparedStatement ps2 = connection.prepareStatement("SELECT "
                            + "first_name, last_name FROM VIPUsers WHERE email=?");
                    ps2.setString(1, rs.getString("executer"));
                    ResultSet rs2 = ps2.executeQuery();
                    String fullNameExecuter = new String();
                    while (rs2.next()) {

                        fullNameExecuter = rs2.getString("first_name") + " " + rs2.getString("last_name");
                    }

                    Timestamp date = rs.getTimestamp("dateExecution");
                    String dateEnd = rs.getString("dateEndExecution");
                    Long id = rs.getLong("queryExecutionID");
                    Long version = rs.getLong("queryVersion");
                    queries.add(new String[]{id.toString(), rs.getString("name"), rs.getString("queryName"), version.toString(), fullNameExecuter, date.toString(), rs.getString("status"), rs.getString("bodyResult"), rs.getString("pathFileResult"), dateEnd});
                }
                ps.close();
                return queries;

            } catch (SQLException ex) {
                logger.error(ex);
                throw new DAOException(ex);
            }
        }
       
    }

    @Override
    //Get body with or no parameter
    public String getBody(Long queryVersionID, Long queryExecutionID, boolean parameter) throws DAOException {
        try {
            if (parameter == false) {
                PreparedStatement ps = connection.prepareStatement("SELECT "
                        + "body FROM QueryVersion WHERE queryVersionID=? ");
                ps.setLong(1, queryVersionID);
                ResultSet rs = ps.executeQuery();
                String body = new String();
                while (rs.next()) {
                    body = rs.getString("body");
              
                }
                ps.close();
                return body;

            } else {

                PreparedStatement ps = connection.prepareStatement("SELECT "
                        + "body, parameterID "
                        + "FROM QueryVersion v, Parameter p "
                        + "WHERE v.queryVersionID = p.queryVersionID AND v.queryVersionID=? ORDER BY p.parameterID DESC");

                ps.setLong(1, queryVersionID);
                ResultSet rs = ps.executeQuery();
                //List<String> value=new ArrayList<String>(); 
                int c = 0;
                int nn = 0;
                String body = null;
                String s = null;

                while (rs.next()) {

                    Long n = rs.getLong("parameterID");
                    if (nn == 0) {
                        body = rs.getString("body");
                        nn = 1;
                    }
                    PreparedStatement ps2 = connection.prepareStatement("SELECT "
                            + "value "
                            + "FROM Value "
                            + "WHERE parameterID=? AND queryExecutionID=?");
                    ps2.setLong(1, n);
                    ps2.setLong(2, queryExecutionID);
                    ResultSet rs2 = ps2.executeQuery();


                    while (rs2.next()) {
                        for (int i = 0; i < body.length(); i++) {
                            char b = body.charAt(i);
                            if (b == '[') {
                                for (int j = i + 1; j < body.length(); j++) {
                                    char last = body.charAt(j);
                                    int kk = 0;
                                    //substring j+1 non inclus

                                    if (last == ']' && kk == 0) {
                                        kk = 1;
                                        c = j + 1;
                                        s = body.substring(i + 1, j);

                                    }
                                }

                            }

                        }
                        body = body.replaceAll("\\[" + s + "\\]", rs2.getString("value"));
                    }

                    ps2.close();



                }


                ps.close();
                return body;
            }


        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }

    }
    //all body of query To check if it exist

    @Override
    public boolean getBodies(Long queryID, String body) throws DAOException {
        try {

            PreparedStatement ps = connection.prepareStatement("SELECT "
                    + "body FROM QueryVersion where queryID=?");

            ps.setLong(1, queryID);
            ResultSet rs = ps.executeQuery();

            int i = 0;
            boolean exist = false;
            while (rs.next() && i == 0) {
                String bd = rs.getString("body");
                bd = bd.replaceAll("\\s", "");
                bd = bd.toLowerCase();

                if (body.equals(bd)) {
                    exist = true;
                    i = 1;
                }
            }


            ps.close();
            return exist;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    private static java.sql.Timestamp getCurrentTimeStamp() {

        java.util.Date today = new java.util.Date();
        return new java.sql.Timestamp(today.getTime());

    }
    public static String html2text(String html) {
    return Jsoup.parse(html).text();
    
}
}
