// Copyright 2017 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.ui.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import google.registry.util.SendEmailService;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SendEmailUtils}. */
@RunWith(JUnit4.class)
public class SendEmailUtilsTest {

  private final SendEmailService emailService = mock(SendEmailService.class);

  private Message message;
  private SendEmailUtils sendEmailUtils;

  @Before
  public void init() {
    message = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
    when(emailService.createMessage()).thenReturn(message);
  }

  private void setRecepients(ImmutableList<String> recepients) {
    sendEmailUtils =
        new SendEmailUtils(
            "outgoing@registry.example",
            "outgoing display name",
            recepients,
            emailService);
  }

  @Test
  public void testSuccess_sendToOneAddress() throws Exception {
    setRecepients(ImmutableList.of("johnny@fakesite.tld"));
    assertThat(sendEmailUtils.hasRecepients()).isTrue();
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isTrue();
    verifyMessageSent();
    assertThat(message.getRecipients(RecipientType.TO)).asList()
        .containsExactly(new InternetAddress("johnny@fakesite.tld"));
    assertThat(message.getAllRecipients()).asList()
        .containsExactly(new InternetAddress("johnny@fakesite.tld"));
  }

  @Test
  public void testSuccess_sendToMultipleAddresses() throws Exception {
    setRecepients(ImmutableList.of("foo@example.com", "bar@example.com"));
    assertThat(sendEmailUtils.hasRecepients()).isTrue();
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isTrue();
    verifyMessageSent();
    assertThat(message.getAllRecipients()).asList().containsExactly(
        new InternetAddress("foo@example.com"),
        new InternetAddress("bar@example.com"));
  }

  @Test
  public void testSuccess_ignoresMalformedEmailAddress() throws Exception {
    setRecepients(ImmutableList.of("foo@example.com", "1iñvalidemail"));
    assertThat(sendEmailUtils.hasRecepients()).isTrue();
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isTrue();
    verifyMessageSent();
    assertThat(message.getAllRecipients()).asList()
        .containsExactly(new InternetAddress("foo@example.com"));
  }

  @Test
  public void testFailure_noAddresses() throws Exception {
    setRecepients(ImmutableList.of());
    assertThat(sendEmailUtils.hasRecepients()).isFalse();
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isFalse();
    verify(emailService, never()).sendMessage(any(Message.class));
  }

  @Test
  public void testFailure_onlyGivenMalformedAddress() throws Exception {
    setRecepients(ImmutableList.of("1iñvalidemail"));
    assertThat(sendEmailUtils.hasRecepients()).isTrue();
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isFalse();
    verify(emailService, never()).sendMessage(any(Message.class));
  }

  @Test
  public void testFailure_exceptionThrownDuringSend() throws Exception {
    setRecepients(ImmutableList.of("foo@example.com"));
    assertThat(sendEmailUtils.hasRecepients()).isTrue();
    doThrow(new MessagingException()).when(emailService).sendMessage(any(Message.class));
    assertThat(
            sendEmailUtils.sendEmail(
                "Welcome to the Internet",
                "It is a dark and scary place."))
        .isFalse();
    verifyMessageSent();
    assertThat(message.getAllRecipients())
        .asList()
        .containsExactly(new InternetAddress("foo@example.com"));
  }

  private void verifyMessageSent() throws Exception {
    verify(emailService).sendMessage(message);
    assertThat(message.getSubject()).isEqualTo("Welcome to the Internet");
    assertThat(message.getContent()).isEqualTo("It is a dark and scary place.");
  }
}
