<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" />
   <xsl:template match="/">
      <html>
         <head>
            <style type="text/css">
                table {
                  width: 85%;
                  border-collapse: collapse;
                  text-align: center;
                }
                .dependencies {
                  text-align: left;
                }
                tr {
                  border: 1px solid black;
                }
                td {
                  border: 1px solid black;
                  font-weight: bold;
                  color: #2E2E2E
                }
                th {
                  border: 1px solid black;
                }
                h3 {
                  text-align:center;
                  margin:3px
                }
                .license {
                    width:70%
                }

                .licenseName {
                    width:15%
                }</style>
         </head>
         <body>
            <table align="center">
               <xsl:choose>
                  <xsl:when test="dependencies">
                     <tr>
                        <th>
                           <h3>Dependency</h3>
                        </th>
                        <th>
                           <h3>File name</h3>
                        </th>
                        <th>
                           <h3>License name</h3>
                        </th>
                        <th>
                           <h3>License text URL</h3>
                        </th>
                     </tr>
                     <xsl:for-each select="dependencies/dependency">
                        <tr>
                           <td class="dependency">
                              <xsl:value-of select="@name" />
                           </td>
                           <td class="fileName">
                              <xsl:value-of select="file" />
                           </td>
                           <td class="licenseName">
                              <xsl:value-of select="license/@name" />
                           </td>
                           <td class="license">
                                <xsl:choose>
                                    <xsl:when test="string-length(license/@url)=0">
                                     No license URL found
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <a>
                                         <xsl:attribute name="href">
                                            <xsl:value-of select="license/@url" />
                                         </xsl:attribute>
                                         Show license agreement
                                      </a>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </td>
                        </tr>
                     </xsl:for-each>
                  </xsl:when>
                  <xsl:otherwise>
                     <tr>
                        <th>
                           <h3>License</h3>
                        </th>
                        <th>
                           <h3>License text URL</h3>
                        </th>
                        <th>
                           <h3>Dependencies</h3>
                        </th>
                     </tr>
                     <xsl:for-each select="licenses/license">
                        <tr>
                           <td class="licenseName">
                              <xsl:value-of select="@name" />
                           </td>
                           <td class="license">
                                 <xsl:choose>
                                    <xsl:when test="string-length(@url)=0">
                                     No license URL found
                                    </xsl:when>
                                    <xsl:otherwise>
                                         <a>
                                          <xsl:attribute name="href">
                                             <xsl:value-of select="@url" />
                                          </xsl:attribute>
                                          License agreement
                                         </a>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </td>
                           <td class="dependencies">
                              <ul>
                                 <xsl:for-each select="dependency">
                                    <li>
                                       <xsl:value-of select="." />
                                    </li>
                                 </xsl:for-each>
                              </ul>
                           </td>
                        </tr>
                     </xsl:for-each>
                  </xsl:otherwise>
               </xsl:choose>
            </table>
         </body>
      </html>
   </xsl:template>
</xsl:stylesheet>