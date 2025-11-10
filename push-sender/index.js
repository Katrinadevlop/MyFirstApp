import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import admin from 'firebase-admin';

// Инициализация Firebase Admin SDK из GOOGLE_APPLICATION_CREDENTIALS
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
} catch (e) {
  console.error('Ошибка инициализации Firebase Admin SDK. Проверьте GOOGLE_APPLICATION_CREDENTIALS.');
  throw e;
}

const argv = yargs(hideBin(process.argv))
  .command(
    'new-post',
    'Отправить уведомление о новом посте',
    (y) =>
      y
        .option('token', { type: 'string', demandOption: true, desc: 'FCM токен устройства' })
        .option('userName', { type: 'string', demandOption: true, desc: 'Имя пользователя' })
        .option('postText', { type: 'string', demandOption: true, desc: 'Текст поста' }),
    async (args) => {
      const message = {
        token: args.token,
        data: {
          action: 'NEW_POST',
          content: JSON.stringify({ userName: args.userName, postText: args.postText }),
        },
      };
      const id = await admin.messaging().send(message);
      console.log('Sent:', id);
    }
  )
  .demandCommand(1)
  .strict()
  .help()
  .parse();
