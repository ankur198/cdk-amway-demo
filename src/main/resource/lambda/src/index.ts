import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda';
import { S3 } from 'aws-sdk';

interface Body {
    feedback: string;
    email: string;
}


export async function handler(event: APIGatewayProxyEvent, context: any): Promise<APIGatewayProxyResult> {
    try {
        const bucketName = process.env.BUCKET_NAME;

        if (!bucketName) {
            throw new Error('BUCKET_NAME is not set');
        }

        // read body from event
        const body = JSON.parse(event.body || '{}') as Body;

        // write to bucket
        const s3 = new S3();
        await s3.putObject({
            Bucket: bucketName,
            Key: `${new Date().toISOString()}.json`,
            Body: JSON.stringify(body),
        }).promise();


        const response: APIGatewayProxyResult = {
            statusCode: 200,
            body: JSON.stringify({ message: 'Feedback received' }),
        };

        return response;
    } catch (error) {
        console.error(error);
        return {
            statusCode: 500,
            body: JSON.stringify({ message: 'Internal server error' }),
        };
    }
}