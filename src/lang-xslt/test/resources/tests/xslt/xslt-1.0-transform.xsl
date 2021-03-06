<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:param name="lorem" select="one"/>

    <xsl:variable name="ipsum" select="two"/>

    <xsl:key name="keys" match="one/two" use="@test"/>

    <xsl:template match="lorem/ipsum" name="test" mode="test">
        <xsl:if test="position() = 1">
            <xsl:copy-of select="@*"/>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="position() = 1">
            </xsl:when>
        </xsl:choose>
        <xsl:number value="position()" count="dolor" from="chapter" format="1. "/>
        <xsl:for-each select="value">
            <xsl:apply-templates select="dolor" mode="test">
                <xsl:sort select="value"/>
                <xsl:with-param name="test" select="sed-emit"/>
            </xsl:apply-templates>
            <xsl:value-of select="position()"/>
        </xsl:for-each>
    </xsl:template>

</xsl:transform>