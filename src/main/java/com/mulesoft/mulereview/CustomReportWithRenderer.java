package com.mulesoft.mulereview;

import java.util.Locale;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.reporting.MavenReportRenderer;

/**
 * Typical code to copy as a reporting plugin start: choose the goal name, then implement getOutputName(),
 * getName( Locale ), getDescription( Locale ) and of course executeReport( Locale ).
 * Notice the implementation of the rendering is done in a separate class to improve separation of concerns
 * and to benefit from helpers.
 */
@Mojo( name = "custom-renderer" )
public class CustomReportWithRenderer
    extends AbstractMavenReport
{
    public String getOutputName()
    {
        return "custom-report-with-renderer";
    }

    public String getName( Locale locale )
    {
        return "Custom Maven Report with Renderer";
    }

    public String getDescription( Locale locale )
    {
        return "Custom Maven Report with Renderer Description";
    }

    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        // use a AbstractMavenReportRenderer subclass to benefit from helpers
        MavenReportRenderer r = new CustomReportRenderer( getSink() );
        r.render();
    }
}