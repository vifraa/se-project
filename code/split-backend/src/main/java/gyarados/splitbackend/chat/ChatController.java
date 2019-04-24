package gyarados.splitbackend.chat;

import gyarados.splitbackend.group.GroupService;
import gyarados.splitbackend.WebSocketEventListener;
import gyarados.splitbackend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * ChatController provides the endpoints for the chat parts of the application.
 */
@Controller
public class ChatController {

    // Logger used to log actions in the controller.
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    // Service to work with chatMessages.
    @Autowired
    private ChatMessageService chatMessageService;


    // Service to work with groups.
    @Autowired
    private GroupService groupService;


    /**
     * findGroup returns an string containing the groupID of the group the user should join. It returns only to the
     * user that sent the initial request.
     * @param user The user that sent the request
     * @return GroupID of a group.
     */
    @MessageMapping("/find-group")
    @SendToUser("/queue/find-group")
    public String findGroup(User user){
        String groupId = groupService.findMatchingGroup(user.getDestinationLatitude(), user.getDestinationLongitude(),user.getCurrentLatitude(), user.getCurrentLongitude());
        logger.info(user.getName() + "got matched with group: " + groupId);

        return groupId;
    }


    /**
     * sendMessage sends an ChatMessage to all connections that are subscribed to the endpoint
     * specified in the @SendTo annotation.
     * @param chatMessage The specified chat message to send.
     * @return The ChatMessage that are being sent.
     */
    @MessageMapping("/chat/{groupId}/sendMessage")
    @SendTo("/topic/{groupId}")
    public ChatMessage sendMessage(@DestinationVariable String groupId, @Payload ChatMessage chatMessage){
        chatMessage.setGroupid(groupId);
        chatMessageService.add(chatMessage);
        groupService.addChatMessageToGroup(groupId, chatMessage);
        logger.info("Message sent: " + chatMessage.toString());
        return chatMessage;
    }

    /**
     * addUser sends an ChatMessage to all connections that are subscribed to the endpoint
     * soecified in the @SendTo annotation. The ChatMessage that is sent is to notify that an
     * user has joined the channel.
     * @param chatMessage The message to be sent.
     * @param headerAccessor object to work with message headers.
     * @return The ChatMessage that are being sent.
     */
    @MessageMapping("/chat/{groupId}/addUser")
    @SendTo("/topic/{groupId}")
    public ChatMessage addUser(@DestinationVariable String groupId, @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor){
        // TODO fix this logic!!!
        // Only placeholder to get the application working until a better fix.
        User user = new User();
        user.setName(chatMessage.getSender());

        groupService.addUserToGroup(groupId, user);
        groupService.addChatMessageToGroup(groupId, chatMessage);
        chatMessage.setGroupid(groupId);
        chatMessageService.add(chatMessage);
        logger.info("User added: " + chatMessage.toString());
        headerAccessor.getSessionAttributes().put("sender", chatMessage.getSender());
        return chatMessage;
    }

}
