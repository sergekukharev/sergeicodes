package com.sergeicodes.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificateProps;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.DistributionProps;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class SergeiCodesStack extends Stack {
    private static final String DOMAIN = "sergeicodes.com";

    public SergeiCodesStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        var domain = HostedZone.fromLookup(this, "Zone",
                HostedZoneProviderProps.builder().domainName(DOMAIN).build());

        var s3main = Bucket.fromBucketName(this, "S3main", "sergeicodes.com");

        var certificate = new DnsValidatedCertificate(this, "SslCert",
                DnsValidatedCertificateProps.builder().domainName(DOMAIN).hostedZone(domain).build());

        var cloudfront = new Distribution(this, "CloudFrontWeb", DistributionProps.builder()
                .comment("sergeicodes.com distribution")
                .defaultBehavior(BehaviorOptions.builder()
                        .origin(new S3Origin(s3main))
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                .certificate(certificate)
                .domainNames(List.of(DOMAIN))
                .defaultRootObject("index.html")
                .enableLogging(true)
                .logBucket(new Bucket(this, "s3logs"))
                .build());

        new ARecord(this, "AliasRecordToCloudfront", ARecordProps.builder()
                .zone(domain)
                .recordName(DOMAIN)
                .target(RecordTarget.fromAlias(new CloudFrontTarget(cloudfront)))
                .build());

        new CfnOutput(this, "CloudFrontDomainName", CfnOutputProps.builder().value(cloudfront.getDistributionDomainName()).build());
        new CfnOutput(this, "CloudFrontDistributionId", CfnOutputProps.builder().value(cloudfront.getDistributionId()).build());
    }
}
