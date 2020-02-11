package com.mulesoft.mulereview;

import java.util.Locale;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * Typical code to copy as a reporting plugin start: choose the goal name, then implement getOutputName(),
 * getName( Locale ), getDescription( Locale ) and of course executeReport( Locale ).
 */
@Mojo( name = "custom" )
public class CustomReport
    extends AbstractMavenReport
{
    public String getOutputName()
    {
        return "custom-report";
    }

    public String getName( Locale locale )
    {
    	getLog().info("Cusatom maven repost");
        return "Custom Maven Report";
    }

    public String getDescription( Locale locale )
    {
        return "Custom Maven Report Description";
    }

    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        // direct report generation using Doxia: compare with CustomReportRenderer to see the benefits of using
        // ReportRenderer
        getSink().head();
        getSink().title();
        getSink().text( "Custom Report Title" );
        getSink().title_();
        getSink().head_();

        getSink().body();

        getSink().section1();
        getSink().sectionTitle1();
        getSink().text( "section" );
        getSink().sectionTitle1_();

        getSink().text( "Custom Maven Report content." );
        getSink().section1_();

        getSink().body_();
    }
}