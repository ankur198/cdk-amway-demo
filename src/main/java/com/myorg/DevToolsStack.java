package com.myorg;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.IgnoreMode;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.Artifacts;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.CodeCommitSourceProps;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxArmLambdaBuildImage;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.S3ArtifactsProps;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.codecommit.Code;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.CodeBuildProject;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.constructs.Construct;

public class DevToolsStack extends Stack {
    public DevToolsStack(Construct construct, String id, StackProps props) {
        super(construct, id, props);

        var codeAsset = Asset.Builder.create(this, "myAsset")
                .path("./src/main/resource/")
                .exclude(List.of("node_modules", "dist"))
                .ignoreMode(IgnoreMode.GIT)
                .build();

        var repo = Repository.Builder.create(this, "myRepo")
                .repositoryName("myRepo")
                .code(Code.fromAsset(codeAsset))
                .build();

        var bucket = Bucket.fromBucketName(this, "assetBucket", "ankurnigam-lambda-s3-demo-bucket");

        var codeBuildLambda = Project.Builder.create(this, "myProjectBuilder")
                .source(Source.codeCommit(CodeCommitSourceProps.builder()
                        .repository(repo)
                        .branchOrRef("refs/heads/main")
                        .build()))
                .artifacts(Artifacts.s3(S3ArtifactsProps.builder()
                        .bucket(bucket)
                        .build()))
                .environment(BuildEnvironment.builder()
                        .computeType(ComputeType.LAMBDA_1GB)
                        .buildImage(LinuxArmLambdaBuildImage.AMAZON_LINUX_2023_NODE_20)
                        .build())
                .buildSpec(BuildSpec.fromSourceFilename("lambda/buildspec.yml"))
                .build();

        var codeCommitRule = Rule.Builder.create(this, "codeCommitRule")
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.codecommit"))
                        .detailType(List.of("CodeCommit Repository State Change"))
                        .resources(List.of(repo.getRepositoryArn()))
                        .detail(Map.of(
                                "event",
                                List.of("referenceCreated", "referenceUpdated"),
                                "referenceType", List.of("branch"),
                                "referenceName", List.of("main")))
                        .build())
                .build();
        codeCommitRule.addTarget(new CodeBuildProject(codeBuildLambda));

        var sourceArtifact = Artifact.artifact("SourceArtifact");
        var buildArtifact = Artifact.artifact("BuildArtifact");

        var pipeline = Pipeline.Builder.create(this, "myPipeline")
                .artifactBucket(bucket)
                .stages(List.of(
                        StageOptions.builder()
                                .stageName("Source")
                                .actions(List.of(
                                        CodeCommitSourceAction.Builder.create()
                                                .actionName("Source")
                                                .output(sourceArtifact)
                                                .repository(repo)
                                                .branch("main")
                                                .build()))
                                .build(),
                        StageOptions.builder()
                                .stageName("Build")
                                .actions(List.of(
                                        CodeBuildAction.Builder.create()
                                                .actionName("Build")
                                                .project(codeBuildLambda)
                                                .input(sourceArtifact)
                                                .outputs(List.of(buildArtifact))
                                                .build()))
                                .build()))
                .build();
    }
}
