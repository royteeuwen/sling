/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.replication.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.resources.ReplicationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle reception of replication content.
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationConstants.IMPORTER_RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = "POST")})
public class ReplicationPackageImporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ReplicationPackageImporter replicationPackageImporter = request
                .getResource()
                .adaptTo(ReplicationPackageImporter.class);

        boolean success = false;
        final long start = System.currentTimeMillis();
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        InputStream stream = request.getInputStream();
        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            ReplicationPackage replicationPackage = replicationPackageImporter.uploadPackage(resourceResolver, stream);
            if (replicationPackage != null) {
                success = replicationPackageImporter.importPackage(resourceResolver, replicationPackage);
                replicationPackage.delete();
            }
            if (!success) {
                log.warn("cannot import replication package from request {}", request);
                response.setStatus(400);
                response.getWriter().print("error: " + (replicationPackage == null ? "could not read a package from the request" :
                        "cannot install package " + replicationPackage));
            }
        } catch (final Exception e) {
            response.setStatus(400);
            log.error("Error during replication import: {}", e.getMessage(), e);
            response.getWriter().print("error: " + e.toString());
        } finally {
            final long end = System.currentTimeMillis();
            log.info("Processed replication request in {}ms: : {}", new Object[]{end - start, success});
        }
    }

}
